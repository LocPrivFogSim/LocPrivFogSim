import sqlite3
import json
import matplotlib.pyplot as plt
from scipy.spatial import Voronoi, voronoi_plot_2d
import numba.np.extensions
from math import sin, cos, sqrt, atan2, radians
from decimal import Decimal
from numba import njit


json_path = "../privacy/output.json"
db_path = "../geoLifePaths.db"
locations_file = "locations.json"


#db
def connect_to_db(path):
    conn = None
    try:
        conn = sqlite3.connect(path)
    except Error as e:
        print(e)
    return conn


#locations
def retrieve_locations(filepath):
    file = open(filepath, 'r+')
    json_arr = file.read()
    k = json.loads(json_arr)
    return k


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




#correctness

#TODO
def calc_correctness(locations_propabilities:list, correct_location):
    sum = 0
    for location in locations_propabilities:
        x = 0 #TODO location coord
        y = 0 #TODO correct_location coord
        distance = calc_dist_in_m(x,y)
        propability = 0 #TODO location Prop
        sum = sum + propability * distance
    

    return sum


def main():
    return



if __name__ == '__main__':
    main()