from datetime import datetime
from geopy import Point
from geopy.distance import geodesic
import math
import json
from helper_methods import *

from scipy.spatial import Voronoi, voronoi_plot_2d
from shapely.geometry import Point as Point2
from shapely.geometry.polygon import Polygon

db_con = connect_to_db()



def createLocationsFile(): #create List of all Locations and serialize it as Json
    corners_peking_field = [[41.229556359747455,116.32212585037877],[40.56688298442188,117.73143927643063],[39.099855,116.519898],[39.75667,115.13559]] #lat,lon
    starting_point = [39.75667,115.13559]  #bottom left corner of peking_field 
    len_x = 139497.82932284995  #metres
    len_y = 193158.51844780458  #metres
    bearing_x = math.degrees(2.1192118132092146)  
    bearing_y = math.degrees(-2.569619036479059+math.pi)

    location_edge_len = 500 #metres

    locations = []
    
    location_points = []

    i = 0.0
    

    counter = 0
    counter2 = 0
    while i < len_x+5000: #some padding
        bot_left = geodesic(meters = i).destination(Point(starting_point[0], starting_point[1]), bearing_x)
        bot_right = geodesic(meters = i + location_edge_len).destination(Point(starting_point[0], starting_point[1]), bearing_x)
        counter2 = counter2 +1 
        j = 0.0
        while j < len_y+5000:
            counter = counter + 1
            top_left = geodesic(meters = location_edge_len).destination(Point(bot_left[0], bot_left[1]), bearing_y)
            top_right = geodesic(meters = location_edge_len).destination(Point(bot_right[0], bot_right[1]), bearing_y)

            locations.append( (list(bot_left), list(top_left), list(top_right), list(bot_right)))

            location_points.append(list(bot_left))

           # print(bot_left, top_left, top_right, bot_right)
            
            bot_left = top_left
            bot_right = top_right
            j = j + location_edge_len

        i = i + location_edge_len

    print(len(locations))
    file = open("json/locations.json",'w+')
    file.write(json.dumps(locations))
    file.close

    file = open("json/locations_points.json",'w+')
    file.write(json.dumps(location_points))
    file.close

    
    print("############  location files created  ################")

    node_positions = select_all_node_positions(db_con)
    location_points = retrieve_locations("json/locations_points.json")
    

    voronoi = Voronoi(node_positions)


    nodes_with_locations = []

    counter=0
    for i in range(len(node_positions)):  
        counter = counter +1 
        if counter%100 == 0 :
            print(counter, "    von", len(node_positions) ,    "    locations länge: ",len(location_points))

        fog_node_id = i
        pos_of_fog_node = node_positions[fog_node_id]
        point_region = voronoi.point_region[fog_node_id]
        vertices = voronoi.regions[point_region]
        vertices_coords = voronoi.vertices[vertices]

        
        polygon = Polygon(vertices_coords)

        locations_in_polygon = []

        for location in location_points:
            distance = calc_dist_in_m(pos_of_fog_node, [location[0],location[1]])
            if(distance > 4000):
                continue

            point = Point2(location[0], location[1])



            if polygon.contains(point):
                locations_in_polygon.append(location)
                location_points.remove(location)
              
        nodes_with_locations.append([fog_node_id, pos_of_fog_node, locations_in_polygon])
        
        
      
    
        
    file = open("json/node_locations.json",'w+')
    file.write(json.dumps(nodes_with_locations))
    file.close()






def main():
    createLocationsFile()
    



if __name__ == '__main__':
    main()