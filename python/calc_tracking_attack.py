from array import array
from datetime import datetime
from msilib import type_key
from random import randrange
from tkinter import E, N
from turtle import distance, position
from helper_methods import *
import pandas as pd
import os
#from math import sqrt
import numpy as np
import math

db_con = connect_to_db()

# The max distance in a 10x10km region is 10000m * sqrt(2)
#max_distance = 10000 * sqrt(2)

max_distance =   sqrt(145000^2 + 200000^2)*1000 #140x194 km rectangle  

# in_data_size    => Tasks input data size
# out_data_size   => Tasks output data size
# mi              => Tasks mi
# position        => Target fog nodes position
# up_bandwidth    => Target fog node upload bandwidth
# down_bandwidth  => Target fog node download bandwidth
# mips            => Target fog node available mips at time t
# sample_point    => Position of the point to test for

@njit
def calc_response_time(in_data_size, out_data_size, mi, position, up_bandwidth, down_bandwidth, mips, sample_point):
    
    #position = numpy.array([numpy.float64(position[0]), numpy.float64(position[1])])
    distance = calc_dist_njit(position, sample_point)
    distance_factor = 1 - (distance / max_distance)
    up_transfere_time = in_data_size / (up_bandwidth * distance_factor)
    calculation_time = mi / mips
    down_transfere_time = out_data_size / (down_bandwidth * distance_factor)
    return up_transfere_time + calculation_time + down_transfere_time


#Pfade kommen aus dem Datensatz?  - ja 
#und findet bei "possible" path schon eine vorauswahl statt? -> Falls ja worüber?
#pfad -> koordinaten Reihenfolge
#Segmente haben eine Gewisse länge in Meter -> möglichst kurz damit davon ausgegangen werden kann, dass jedes Segment eine Geschwindigkeit x hat
#Wie sollen die Segmente gebildet werden? -> Idee: Sliding Window über Pfad und jeweils Segmente bei denen die Dauer passt ( Länger als getrackte Sequenz)
#Zeile 6: erst hier möglich, da erst geschwindigkeiten bekannt


#tracked fog node = [timestamp, node_id, amount_of_data_transferred]
def calc_tracking_attack(path_data, locations):

    events = path_data[2]
    events = [event for event in events if event['event_name'] == "add"]
  
    tracked_duration = events[-1]['timestamp'] #TODO (last tracked_fog_nodes[timestamp] - first )
  
    number_of_observations = len(events) - 1 #k

    possible_paths = [] #TODD heuristic selection probably needed

    example_path = select_path_from_db(db_con, 3)


    example_path = path_as_dict(example_path)


    possible_paths.append(example_path)
    #possible_paths.append(example_path2)


    for path in possible_paths:
        alpha = 0

        path_coords = np.array([[float(coord[0]), float(coord[1])]  for coord in  path['path_coords']])
        len_of_segments = 5 # in metres
        nr_of_segments = math.ceil(path['distance']/len_of_segments)
        segments, coords,comparison = divide_path_into_segments(path_coords, len_of_segments, nr_of_segments)  #divide path into path into segments P1... Pc
        
        #for i in range(100):
        #    print(segments[i])

        #print("#######START1######")

        f =  open("original_path.gpx", 'w+')
        f.write(createGPX(coords))
        f.close
        #print(createGPX(coords))
        #print("#######Start2######")
        f =  open("segmented_path.gpx", 'w+')
        f.write(createGPX(comparison))
        f.close

        nr_iterations = 100 #TODO probably increase

        for j in range(nr_iterations):
            

            segments = sample_velocities(segments)
            time_for_traversing = sum([ segment[6] for segment in segments])
            
            print("tracked_time: ", tracked_duration)
            print("time total: ", time_for_traversing)

            if time_for_traversing > tracked_duration:
                beta = 1

               t_0 = select_random_t0()
                    

                for segment in reversed(segments):
                    while 



    return 0 #TODO return probability distribution over the set of possible paths


#returns list of [segment_start_lat, segment_start_lon, segment_end_lat , segment_end_lon, distance, velocity,traversing_time], distance = len_of_segments for all but the last element
@njit
def divide_path_into_segments(coords, len_of_segments:int, nr_of_segments):

    #print(coords)
    #print("---------")
    segment_arr = np.zeros((nr_of_segments,7))
    
    segment_i = 0

    segment_start = coords[0]

    left_till_segment_length = len_of_segments


    for i in range(1, len(coords)): 
        x = coords[i-1]
        y = coords[i]

        
        
        distance = calc_dist_njit(x,y)

        while(left_till_segment_length < distance): #segment_end is between x and y 
            
            vec = y - x
            segment_end = x + (left_till_segment_length/distance)*vec

            segment_arr[segment_i][0] = segment_start[0]   # start_lat
            segment_arr[segment_i][1] = segment_start[1]   #  start_lon
            segment_arr[segment_i][2] = segment_end[0]          #  end_lat
            segment_arr[segment_i][3] = segment_end[1]          #  end_lon
            segment_arr[segment_i][4] = len_of_segments         # dist

            segment_i += 1

            x = segment_end
            segment_start = segment_end
            
            distance -= left_till_segment_length
            left_till_segment_length = len_of_segments


        left_till_segment_length -= distance



    # add last section (with distance < len_of_segments)
    final_coord = coords[-1]
    segment_arr[segment_i][0] = segment_start[0]   # start_lat
    segment_arr[segment_i][1] = segment_start[1]   #  start_lon
    segment_arr[segment_i][2] = final_coord[0]          #  end_lat
    segment_arr[segment_i][3] = final_coord[1]          #  end_lon
    
    distance = calc_dist_njit(segment_start, final_coord) # distance
    segment_arr[segment_i][4] = distance  


    comparison = []
    for x in segment_arr:
        #a = [x[0], x[1]]
        b = [x[2], x[3]]

        #if a not in comparison:
        #    comparison.append(a)
        if b not in comparison:
            comparison.append(b)

    

    return segment_arr, coords, comparison

@njit
def sample_velocities(segments):
    for segment in segments:
        velocity = (randrange(4,7)*1000)/(60*60)
        segment[5] = velocity        # velocity TODO
        segment[6] = segment[4]/velocity   # traversing_time
    return segments

def createGPX(coords: list):
    # header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?> \n\n"
    follow = "<gpx>\n<trk>\n<trkseg>\n"
    end = "</trkseg>\n</trk>\n</gpx>"

    coordstmp = ""
    for coord1 in coords:
        lat1 = str(coord1[0])
        lon1 = str(coord1[1])

        

        coordstmp = coordstmp + "<trkpt lat=\"" + lat1 + "\" lon=\"" + lon1 + "\"> </trkpt>\n"

    content = follow + coordstmp + end
    # print(content)
    return content


def calc_strategy_fastest(path_data, locations:list):

    #path_data [path_id, compromised_fog_nodes, events, fog_device_infos, device_stats]
    #fog_device_infos [fog_device_id, downlink_bandwidth, uplink_bandwidth, uplink_latency]
    #event [fog_device_id, event_name, event_type, event_id, timestamp, availableMips, taskID, dataSize, mi, maxMips]
    #device_stats []
    path_id = path_data[0]

    path_coords = get_path_coordinates_from_db(db_con, path_id)


    compromised_fog_nodes = path_data[1]

    events = path_data[2]  #event{ fog_device_id, event_name, event_type, event_id, timestamp, consideredFogNodes, consideredField, ...}
    
    fog_device_infos = path_data[3]

    device_stats = path_data[4]


    fog_device_positions = select_all_node_positions(db_con)

    total_correctness = 0
    avg_corr = 0

    #for each location check which fog node is the fastest to respond
    current_min = 1000000000 #some high nr

    
    add_event = None
    remove_event = None

    counted_events = 0
 
    
    # remove all events that dont belong to a compromised fog_node
    events = [event for event in events if event['fog_device_id'] in compromised_fog_nodes]

    #clean data into np arrays
    #location, add_event, remove_event, fog_device_infos, device_stats, fog_device_positions, selected_fog_node_position

    for event in events:
        fog_device_id = event['fog_device_id']
        selected_fog_node_position = fog_device_positions[fog_device_id]


        if fog_device_id not in compromised_fog_nodes:
            continue
        
        event_name = event['event_name']
        if (event_name == "add"):
            add_event = event

        if (event_name == "remove"):
            remove_event= event

        if remove_event is None or add_event is None:
            continue

        if add_event['taskId'] != remove_event['taskId']:
            continue
        
        
        #print("######## new event ##############")

        timestamp = event['timestamp']


        
        
        actual_position = get_position_for_timestamp(path_coords, timestamp)

        counted_events += 1

        #get relevant locations    
        edges = [[edge_point['lat'], edge_point['lon']]  for edge_point in event['consideredField']]
        edges = np.asarray(edges)

        relevant_locations = [np.array([l[0], l[1]]) for l in locations if calc_dist_in_m(selected_fog_node_position, l) < 10000 * sqrt(2)] 
        relevant_locations = [ l for l in relevant_locations if ray_tracing(l[0], l[1], edges)] #only get locations inside 10x10km square

        
        #print("selected_fn_position: ",selected_fog_node_position)
        #print("edgepoints: ",edges)
        #print("example relevant pos: ",relevant_locations[0])
        #print("eventId: ", event['event_id'])
        
        considered_fog_devices = event['consideredFogNodes']
        
        prob_location= 1/len(relevant_locations)        #Pr(ℓ) = 1/|L|
        prob_fog_node = 1/len(considered_fog_devices)   #Pr(f∗) = 1/|F|

        threshold_distr = {}  #key=threshold, val=prob

        for location in relevant_locations:
           
            for threshold in threshold_distr.keys():
                cond_probability_node = cond_prob_select_node(location, add_event, remove_event, fog_device_infos, device_stats, fog_device_positions, considered_fog_devices, threshold)
                prob = threshold_distr[threshold]

                #Todo Integral (Formel 9)

        possible_locations = [] # TODO

        for location in possible_locations:
            distance = calc_dist_in_m(location, actual_position)
            #print(distance)
            correctness = probability * distance
            total_correctness += correctness

    if(counted_events == 0):
        return 0,0     
    
    avg_corr = total_correctness/counted_events

    print ("avg_corr: ",avg_corr, "      total: ",total_correctness)

    return total_correctness, avg_corr


#Pr(f∗ |ℓ,x)  = 1/|ˆF(ℓ,x)| if f∗ ∈ ˆF(ℓ,x   || 0 otherwise 
#conditional prob that node f* is selected from location l for threshold x
def cond_prob_select_node(location, add_event, remove_event, fog_device_infos, device_stats, fog_device_positions, considered_fog_devices,selected_fog_node_id, threshold):
    in_data_size = add_event['dataSize']
    out_data_size = remove_event['dataSize']
    mi = add_event['mi']
    sample_point = numpy.array([numpy.float64(location[0]), numpy.float64(location[1])])
    base_mips = add_event['maxMips']

    task_id = add_event['taskId']

    #base_mips = max(device_stats[task_id].values())
    #min_mips = min(device_stats[task_id].values())

    device_stats_keys = device_stats[task_id].keys()


    nodes_with_rt_below_threshold = []


    for i in considered_fog_devices: 
        
        mips = base_mips
        position = fog_device_positions[i]
        #position = numpy.array([numpy.float64(position[0]), numpy.float64(position[1])])
        device = fog_device_infos["fog_device_id" == i]     
        up_bandwidth = device['uplink_bandwidth']
        down_bandwidth = device['downlink_bandwidth']

        if i in device_stats_keys:
            mips = device_stats[i]
         
        response_time = calc_response_time(in_data_size, out_data_size, mi, position, up_bandwidth, down_bandwidth, mips, sample_point)

        if response_time <= threshold:
            nodes_with_rt_below_threshold.append(i)    

    if len(nodes_with_rt_below_threshold) == 0:
        return 0

    if selected_fog_node_id not in nodes_with_rt_below_threshold:
        return 0
    
    return 1/len(nodes_with_rt_below_threshold)  #1/|ˆF(ℓ,x)



def main():
    result_file_path = "results/tracking_attack.csv"

    input_json_dir = "input/Strategie_1"    #Todo

    locations= retrieve_list_from_json("json/locations_points.json")#[nodeid, node_position,locations]

     #clear result file
    f =  open(result_file_path, 'w+')
    f.close

    df = pd.DataFrame(columns=['strategy','rate','iteration','total_correctness','avg_correctness'])

     #iterate input files
    for dirpath, dirs, files in os.walk(input_json_dir):
        for file in files:
            time0 = datetime.now()
            print(dirpath, "     - file: ",file)
            input_file = os.path.join(dirpath,file)

    
            total_correctness,avg_corr = calc_tracking_attack((retrieve_data_from_json(input_file)), locations)

            #total_correctness,avg_corr= calc_strategy_fastest(retrieve_data_from_json(input_file),locations)
            file_split = (str(file)).split('_') # e.g. ['output', '3', '100', '1.json']
            strat = file_split[1]
            rate = file_split[2]
            iteration = file_split[3].split('.')[0]

            df = df.append({'strategy':strat, 'rate':rate, 'iteration':iteration, 'total_correctness':total_correctness,'avg_correctness':avg_corr}, ignore_index=True)
            time1 = datetime.now()

            print("\n one file took:  ",str(time1-time0), " \n\n")
            
    df.to_csv(result_file_path) 
    return



if __name__ == '__main__':
    main()