from cmath import pi
from re import X
import sqlite3
import json
import numpy
import numba.np.extensions
from math import degrees, sin, asin, atan,  cos, sqrt, atan2, radians
from decimal import Decimal
from numba import njit
from numba import types
from numba.typed import Dict
from geopy.distance import geodesic
from geopy import Point



db_path = "../geoLifePaths.db"
locations_file = "locations.json"

float64_array = types.float64[:]

#db
def connect_to_db():
    conn = None
    try:
        conn = sqlite3.connect(db_path)
    except Error as e:
        print(e)
    return conn

def select_all_node_positions(conn):
    cursor = conn.cursor()
    cursor.execute("SELECT lat, lon FROM node_positions")
    node_positions = cursor.fetchall()
    return node_positions


def select_path_from_db(conn, path_id):
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM paths WHERE path_id == " + str(path_id))

    rows = cursor.fetchall()
    
    if(len(rows) != 1):
        raise ValueError( str(len(rows)) + ' paths (!=1) selected! in metricsCalculator.select_path_from_db()') 

    path = rows[0]

    return path


def get_path_distance(conn, path_id):
    path = select_path_from_db(conn, path_id)
    print(path)
    exit()


def get_path_coordinates_from_db(conn, path_id):
    full_path = select_path_from_db(conn, path_id)
    return format_path_coordinates(full_path[1])

def format_path_coordinates(full_path):
    coords_string = full_path
    coords_list = []
    split_string = coords_string.split("||")
    x = []
    for s in split_string:
        if len(s) != 0:
            split_inner = s.split(",")
            x.append([split_inner[0], split_inner[1], split_inner[2]])
    return x

def path_as_dict(path_before):
    path = {
        'id': path_before[0],
        'path_coords': format_path_coordinates(path_before[1]),
        'distance': path_before[3],
        'fog_nodes_trace' : path_before[-1]
    }
    

    return path

def get_position_for_timestamp(path, timestamp):
    for coord in path:
        if coord[2] == str(timestamp) :
            return coord
    

@njit
def get_relevant_locations(locations, node_pos, edges):
    threshold = 10000 * sqrt(2)
    i = 0
    arr = numpy.empty(shape=(len(locations),2))

    for l in locations:
        if calc_dist_njit(node_pos, l) < threshold:
            if ray_tracing(l[0], l[1], edges):
                arr[i] = [l[0], l[1]] 
                i += 1

    arr1 = arr[0:i]  #sliced array 
   
    return arr1

def trans_device_infos(device_infos):
    devices_map = Dict.empty(
        key_type=types.int64,
        value_type=float64_array
    )

    for device in  device_infos:

        dw_bandw = float(device['downlink_bandwidth'])
        up_bandw = float(device['uplink_bandwidth'])
        uplink_latency = float(device['uplink_latency'])
        devices_map[device["fog_device_id"]] = numpy.array([dw_bandw, up_bandw, uplink_latency])

    return devices_map

def get_coords_dict(coords):
    
    coords_map = Dict.empty(
        key_type=types.float64,
        value_type=float64_array
    )

    for coord in coords:
        ts = float(coord[2])
        lat = float(coord[0])
        lon = float (coord[1])
        coords_map[ts] = numpy.array([lat, lon])
    return coords_map

    
def get_observed_order_of_fognodes(events):
    """
    eg. fog_device_ids from one example json:
    [1100, 1100, 1100, 999, 999, 200, 300] -> returns [1100, 999, 200, 300]
    """
    
    order = []
    
    for i in range(len(events)):
        fog_device_id = events[i]['fog_device_id']

        if(len(order) == 0):
            order.append(fog_device_id)
            continue

        if(order[len(order) -1 ]   != fog_device_id):
            order.append(fog_device_id)

    if(len(order) == 0):
        raise ValueError( 'order is empty! in metricsCalculator.observed_order_of_fognodes()') 

    return order


#locations
def retrieve_list_from_json(filepath):
    file = open(filepath, 'r+')
    json_arr = file.read()
    k = json.loads(json_arr)
    return k



# [path_id, simulatedScenario, compromised_fog_nodes, events]
#each event { fog_device_id, event_name, event_type, event_id, timestamp }
def retrieve_data_from_json(jsonpath):
     with open(jsonpath) as json_file:
        data = json.load(json_file)
   
     path_id = data['simulatedPath']
     #simulatedScenario = data['simulatedScenario']
     compromised_fog_nodes = data['compromisedFogNodes']
     events = data['events']     
     fog_device_infos = data['fogDeviceInfos']
     device_stats = data['deviceStats']
     return [path_id, compromised_fog_nodes, events, fog_device_infos, device_stats]

#distances 

def calc_dist_in_m(coordinate1, coordinate2):
    x = numpy.array([numpy.float64(coordinate1[0]), numpy.float64(coordinate1[1])])
    y = numpy.array([numpy.float64(coordinate2[0]), numpy.float64(coordinate2[1])])
    return calc_dist_njit(x, y)

@njit()
def calc_dist_njit(x, y):

    radius_earth = 6371.0

    lat1 = radians(x[0])
    lon1 = radians(x[1])
    lat2 = radians(y[0])
    lon2 = radians(y[1])

    dlon = lon2 - lon1
    dlat = lat2 - lat1

    a = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
    c = 2 * atan2(sqrt(a), sqrt(1 - a))

    distance = radius_earth * c * 1000
    return distance

@njit()
def test_distance(lat1, lon1, lat2, lon2):
    p = 0.017453292519943295
    a = 0.5 - cos((lat2 - lat1) * p)/2 + cos(lat1 * p) * cos(lat2 * p) * (1 - cos((lon2 - lon1) * p)) / 2
    return 1000 * 12742 * asin(sqrt(a))

@njit
def get_bearing(lat1,lon1,lat2,lon2):
    lat1 = numpy.deg2rad(lat1)
    lon1 = numpy.deg2rad(lon1)
    lat2 = numpy.deg2rad(lat2)
    lon2 = numpy.deg2rad(lon2)
    dLon = lon2 - lon1
    y = sin(dLon) * cos(lat2)
    x = cos(lat1)*sin(lat2) - sin(lat1)*cos(lat2)*cos(dLon)
    brng = numpy.rad2deg(atan2(y, x))
    if brng < 0: brng+= 360
    
    print("brn: ",brng)

    return brng


def calc_destination_between_points(start, end, distance):
    print("Start ",start[0])
    bearing = get_bearing(start[0], start[1], end[0], end[1])
    print("geod: ",geodesic(meters = distance*1000).destination(Point(start[0], start[1]), bearing).latitude,geodesic(meters = distance).destination(Point(start[0], start[1]), bearing).longitude )
    print("sec: ", calc_destination_for_bearing(start[0], start[1], bearing, distance))
    return calc_destination_for_bearing(start[0], start[1], bearing, distance)


@njit
def calc_destination_for_bearing(lat1, lon1, bearing, distance):
    radius = 6371
    lat1 = radians(lat1)
    lon1 = radians(lon1)

    d_div_r = float(distance) / radius

    lat2 = asin(
        sin(lat1) * cos(d_div_r) +
        cos(lat1) * sin(d_div_r) * cos(radians(bearing))
    )

    lon2 = lon1 + atan2(
        sin(bearing) * sin(d_div_r) * cos(lat1),
        cos(d_div_r) - sin(lat1) * sin(lat2)
    )       

    

    return[degrees(lat2), degrees(lon2)]

#checks if point(x,y) is inside of polygon poly
@njit
def ray_tracing(x,y,poly):
    n = len(poly)
    inside = False
    p2x = 0.0
    p2y = 0.0
    xints = 0.0
    p1x,p1y = poly[0]
    for i in range(n+1):
        p2x,p2y = poly[i % n]
        if y > min(p1y,p2y):
            if y <= max(p1y,p2y):
                if x <= max(p1x,p2x):
                    if p1y != p2y:
                        xints = (y-p1y)*(p2x-p1x)/(p2y-p1y)+p1x
                    if p1x == p2x or x <= xints:
                        inside = not inside
        p1x,p1y = p2x,p2y

    return inside


#correctness
#TODO
def calc_correctness(locations_propabilities:list, correct_location):
    sum = 0
    for location in locations_propabilities:
        x = location[0] 
        y = correct_location
        distance = calc_dist_in_m(x,y)
        propability = location[1] 
        sum = sum + propability * distance
    

    return sum


def dtw(path_X, path_Y):
    x = []
    y = []
    for i in range(len(path_X)):
        typedA = numpy.float64(path_X[i][0])
        typedB = numpy.float64(path_X[i][1])
        x.append([typedA, typedB])

    for i in range(len(path_Y)):
        typedA = numpy.float64(path_Y[i][0])
        typedB = numpy.float64(path_Y[i][1])
        y.append([typedA, typedB])

    npArrA = numpy.array(x)
    npArrB = numpy.array(y)

    return dtw_njit(npArrA, npArrB)


@njit()
def dtw_njit(path_X, path_Y):
    X = len(path_X)
    Y = len(path_Y)

    # store distances for all pairs of both paths in matrix
    distance_mat = numpy.zeros((X, Y))

    for i in range(X):
        for j in range(Y):
            distance_mat[i, j] = calc_dist_njit(path_X[i], path_Y[j])

    # init cost matrix
    dist_mat = numpy.zeros((X + 1, Y + 1))
    for i in range(1, X + 1):
        dist_mat[i, 0] = numpy.inf
    for i in range(1, Y + 1):
        dist_mat[0, i] = numpy.inf

    # fill cost matrix
    for i in range(1, X + 1):
        for j in range(1, Y + 1):
            loc_dist = distance_mat[i - 1, j - 1]
            dist_mat[i, j] = loc_dist + min([dist_mat[i - 1, j - 1], dist_mat[i, j - 1], dist_mat[i - 1, j]])

    return dist_mat[X, Y]



def main():
    return



if __name__ == '__main__':
    main()