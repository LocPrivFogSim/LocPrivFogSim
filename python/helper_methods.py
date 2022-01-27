from re import X
import sqlite3
import json
import numpy
import numba.np.extensions
from math import sin, cos, sqrt, atan2, radians
from decimal import Decimal
from numba import njit


db_path = "../geoLifePaths.db"
locations_file = "locations.json"


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

def get_path_coordinates_from_db(conn, path_id):
    full_path = select_path_from_db(conn, path_id)
    coords_string = full_path[1]
    coords_list = []
    split_string = coords_string.split("||")
    x = []
    for s in split_string:
        if len(s) != 0:
            split_inner = s.split(",")
            x.append([split_inner[0], split_inner[1], split_inner[2]])
    return x


def get_position_for_timestamp(path, timestamp):
    for coord in path:
        if coord[2] == str(timestamp) :
            return coord
    


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
def retrieve_locations(filepath):
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
     return [path_id, compromised_fog_nodes, events]

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


def calc_response_time():
    return 100


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



def main():
    return



if __name__ == '__main__':
    main()