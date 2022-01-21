from geopy import Point
from geopy.distance import geodesic
import math
import json

def createLocationsFile(): #create List of all Locations and serialize it as Json
    corners_peking_field = [[41.229556359747455,116.32212585037877],[40.56688298442188,117.73143927643063],[39.099855,116.519898],[39.75667,115.13559]] #lat,lon
    starting_point = [39.75667,115.13559]  #bottom left corner of peking_field 
    len_x = 139497.82932284995  #metres
    len_y = 193158.51844780458  #metres
    bearing_x = math.degrees(2.1192118132092146)  
    bearing_y = math.degrees(-2.569619036479059+math.pi)

    location_edge_len = 500 #metres

    locations = []

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

           # print(bot_left, top_left, top_right, bot_right)
            
            bot_left = top_left
            bot_right = top_right
            j = j + location_edge_len

        i = i + location_edge_len

    print(len(locations))
    file = open("locations.json",'w+')
    file.write(json.dumps(locations))
    file.close
    


def debugPrint():
    return 0



def main():
    bearing = math.degrees(-2.569619036479059+math.pi)
    print(bearing)
    x = geodesic(meters=10000).destination(Point(39.75667,115.13559), bearing)
    print(x.format_decimal)
    print(x[0])
    print(x[1])
    
    print(list(x))

    print("/n/n/n#######################################################/n/n/n")

    createLocationsFile()

if __name__ == '__main__':
    main()