from array import array
from datetime import datetime, timedelta
import pstats
from random import randrange
from helper_methods import *
#from calc_fastest import get_fastest_comp_fog_node
import pandas as pd
import os
#from math import sqrt
import numpy as np
import math

from numba import njit, jit
from numba.core import types
from numba.typed import Dict

import cProfile



#Arraytype for numba
float_array = types.float64[:]



db_con = connect_to_db()

rel_locations_for_node = {} 


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
 
@njit(parallel = True)
def calc_response_time(in_data_size, out_data_size, mi, position, up_bandwidth, down_bandwidth, mips, sample_point):
    
    #position = numpy.array([numpy.float64(position[0]), numpy.float64(position[1])])
    #distance1 = calc_dist_njit(position, sample_point)
    distance = test_distance(position[0], position[1], sample_point[0], sample_point[1])
    distance_factor = 1 - (distance / max_distance)
    up_transfere_time = in_data_size / (up_bandwidth * distance_factor)
    calculation_time = mi / mips
    down_transfere_time = out_data_size / (down_bandwidth * distance_factor)
    return up_transfere_time + calculation_time + down_transfere_time


#tracked fog node = [timestamp, node_id, amount_of_data_transferred]

def calc_tracking_attack(path_data, locations, strategy_id):
    #path_data [path_id, compromised_fog_nodes, events, fog_device_infos, device_stats]

    locations = np.array(locations)

    actual_path_id = path_data[0]
    actual_coords = get_path_coordinates_from_db(db_con, actual_path_id)
    compromised_fog_nodes = path_data[1]
    events = path_data[2]
    fog_device_infos = path_data[3]
    device_stats = path_data[4]

    fog_device_positions = select_all_node_positions(db_con)
    fog_device_positions = np.array(fog_device_positions)

    # remove all events that dont belong to a compromised fog_node
    events = [event for event in events if event['fog_device_id'] in compromised_fog_nodes]
    
    if len(events) == 0:
        return 0,0
    
    tracked_duration = events[-1]['timestamp'] #TODO (last tracked_fog_nodes[timestamp] - first )
    number_of_observations = len(events)  #k

    debug_path_heuristik = 0 #TODO remove


    path_prob = {}

    for path_id in range(37000,50000):

        path = select_path_from_db(db_con, path_id)
        path = path_as_dict(path)

        alpha = 0

        path_coords = np.array([[float(coord[0]), float(coord[1])]  for coord in  path['path_coords']])

        if calc_dist_in_m(path_coords[0], actual_coords[0]) > 5000: #heuristic to reduce runtime
            continue
        
        debug_path_heuristik += 1

        print("path nr: ", path_id)

        len_of_segments = 15 # in metres
        nr_of_segments = math.ceil(path['distance']/len_of_segments)
        
        segments = divide_path_into_segments(path_coords, len_of_segments, nr_of_segments)  #divide path into path into segments P1... Pc
        
        nr_iterations = 100 #TODO probably increase

        for j in range(nr_iterations):
            
            
            segments = sample_velocities(segments)
            time_for_traversing = sum([ segment[6] for segment in segments])
            
            if time_for_traversing > tracked_duration:
                beta = 1
                
                max_t0 = int(time_for_traversing - tracked_duration)
                rand_t0 = randrange(0,max_t0)

                segments_dict = get_segments_dict(segments, rand_t0, tracked_duration) 

                timestamps = [event['timestamp'] for event in events]
                timestamps = np.array(list(dict.fromkeys(timestamps)))  #remove duplicates

             
                locations_on_path = get_locations_at_ts(segments_dict, timestamps)
           
                for i in range(int(number_of_observations/2)):
                    e = events[i*2]

                    selected_fog_node = e['fog_device_id']

                    timestamp = events[i]["timestamp"]
                    guessed_location = locations_on_path[timestamp]

                    actual_position = get_position_for_timestamp(actual_coords, timestamp)
                    actual_position = np.array([float(actual_position[0]), float(actual_position[1])])

                    strategy_id = int(strategy_id)

                    prob_for_location = 0
                   
                    # - strat = 1: BelowThresholdRandomDevice
                    # - strat = 2: BelowThresholdLowestResponseTime
                    # - strat = 3: ClosestFogDevice
                    if strategy_id ==1 :
                        prob_for_location = prob_not_slow(guessed_location, e, events[i+1])
                    
                    if strategy_id ==2 :
                        prob_for_location = prob_fastest(guessed_location, actual_position, e, events[i+1], fog_device_infos, device_stats, fog_device_positions, locations)

                    if strategy_id ==3 :
                        prob_for_location = prob_clostest(guessed_location, e)
                    
                    beta = beta * prob_for_location

                alpha += beta

        if alpha > 0:
            path_prob[path_id] = alpha


    print("Anzahl untersuchter Pfade: ",debug_path_heuristik)
    print("\n\n DONEEEE")


    print(path_prob)

    
    return 100,0 #TODO return probability distribution over the set of possible paths


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


    return segment_arr


@njit
def sample_velocities(segments):
    for segment in segments:
        velocity = (randrange(4,7)*1000)/(60*60)
        segment[5] = velocity        # velocity TODO
        segment[6] = segment[4]/velocity   # traversing_time
    return segments

@njit
def get_segments_dict(segments, t_0, duration):

    segments_map = Dict.empty(
        key_type=types.float64,
        value_type=float_array
    )
    sum_times = 0
    first_elem = 0
    for i in  range(len(segments)):
        first_elem = i
        sum_times += segments[i][6]
        if(sum_times < t_0):
            continue
        sum_times = 0
        break
    
    for j in range(first_elem,len(segments)):
        segments_map[sum_times] = segments[j]
        
        if (sum_times > duration):
            break
        sum_times += segments[j][6]

    #print(segments_map)
    #print(next(iter(segments_map)))
    return segments_map

@njit
def get_locations_at_ts(segments_dict, timestamps):

    locations_dict = Dict.empty(
        key_type=types.int64,
        value_type=float_array
    )

    keys = list(segments_dict.keys())     

    i = 0

    for j in range(len(timestamps)-1):
        
     
        

        if i+1 >= len(keys):
            break
      
        

        while not keys[i] > timestamps[j]: #timestamp relates to point on current segment
            i+=1
            if i+1 >= len(keys):
                break
            
       


        ts = 0

        timestamp = timestamps[j]

        key = keys[i-1]



        #print(timestamps)
        #print(keys)
        #print(timestamp)
        #print(key)
        #return None
        segment = segments_dict[key]
        try:
            segment_start_lat = segment[0]
            segment_start_lon = segment[1]
            segment_end_lat = segment[2]
            segment_end_lon = segment[3]
            traversing_time = segment[6]
        except Exception:
            print("e")

        start_coord = np.array([segment_start_lat, segment_start_lon])
        end_coord = np.array([segment_end_lat, segment_end_lon])

        seg_vector = end_coord - start_coord
        x = (timestamp-key) / traversing_time
            
        dest_point = start_coord + seg_vector * x
                
        locations_dict[timestamp] = dest_point
        
          

    return locations_dict



def prob_not_slow(guessed_location, actual_position, current_add_event, next_remove_event, fog_device_infos, device_stats, fog_device_positions, locations):

    #


    return 0

def prob_fastest(guessed_location, actual_position, current_add_event, next_remove_event, fog_device_infos, device_stats, fog_device_positions, locations):
    t1 = datetime.now()

    if calc_dist_njit(guessed_location, actual_position) > 10000 * sqrt(2):
        return 0

    actual_device = current_add_event['fog_device_id']
    actual_device_position = np.array(fog_device_positions[actual_device])

    #location has to be in 10*10 square  
    edges = [[edge_point['lat'], edge_point['lon']]  for edge_point in current_add_event['consideredField']]
    edges = np.asarray(edges)

    
    guessed_location = np.array([guessed_location[0], guessed_location[1]])

    if not ray_tracing(guessed_location[0], guessed_location[1], edges):
        return 0    


    #if relev 
    if actual_device not in rel_locations_for_node.keys():
        relevant_locations = get_relevant_locations(locations, actual_device_position, edges)
        rel_locations_for_node[actual_device] = relevant_locations

    relevant_locations = rel_locations_for_node[actual_device]
  
    considered_fog_devices = current_add_event['consideredFogNodes']
    

    chosen_device = get_fastest_comp_fog_node(guessed_location, current_add_event, next_remove_event, fog_device_infos, device_stats, fog_device_positions, considered_fog_devices)
    
    if chosen_device != actual_device:
        return 0

    possible_locations = []

    for location in relevant_locations:
        chosen_device = get_fastest_comp_fog_node(location, current_add_event, next_remove_event, fog_device_infos, device_stats, fog_device_positions, considered_fog_devices)
        if chosen_device == actual_device:
            possible_locations.append(location)
    

    #print("weitere mögliche Positionen: ",possible_locations)

    return 1/(len(possible_locations) + 1)

@njit
def get_relevant_locations(locations, node_pos, edges):
    threshold = 10000 * sqrt(2)
    i = 0
    arr = np.empty(shape=(len(locations),2))

    for l in locations:
        if calc_dist_njit(node_pos, l) < threshold:
            if ray_tracing(l[0], l[1], edges):
                arr[i] = [l[0], l[1]] 
                i += 1

    arr1 = arr[0:i]  #sliced array 
   
    return arr1
   

@jit
def get_fastest_comp_fog_node(location, add_event, remove_event, fog_device_infos, device_stats, fog_device_positions, considered_fog_devices):
 
    in_data_size = add_event['dataSize']
    out_data_size = remove_event['dataSize']
    mi = add_event['mi']
    sample_point = numpy.array([numpy.float64(location[0]), numpy.float64(location[1])])
    base_mips = add_event['maxMips']

    task_id = add_event['taskId']

    base_mips = max(device_stats[task_id].values())
 
    device_stats_keys = device_stats[task_id].keys()

    current_min_rt = 100000000000
    fastest_node = None

    for i in range(len(considered_fog_devices)): 
      
        mips = base_mips
        position = fog_device_positions[i]
        #position = numpy.array([numpy.float64(position[0]), numpy.float64(position[1])])
        device = fog_device_infos["fog_device_id" == considered_fog_devices[i]]  

        up_bandwidth = device['uplink_bandwidth']
        down_bandwidth = device['downlink_bandwidth']

        if i in device_stats_keys:
            mips = device_stats[considered_fog_devices[i]]
        
        #response_time = calc_response_time(in_data_size, out_data_size, mi, position, up_bandwidth, down_bandwidth, mips, sample_point)
        response_time = 0
        if response_time < current_min_rt:
            current_min_rt = response_time
            fastest_node = considered_fog_devices[i]

    return fastest_node



def prob_clostest(guessed_location, current_event, locations):
    return 0


#for Debugging
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



def main():
    result_file_path = "results/tracking_attack.csv"

    input_json_dir = "input/Strategie_2"    #Todo

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

    

            #total_correctness,avg_corr= calc_strategy_fastest(retrieve_data_from_json(input_file),locations)
            file_split = (str(file)).split('_') # e.g. ['output', '3', '100', '1.json']
            strat = file_split[1]
            rate = file_split[2]
            iteration = file_split[3].split('.')[0]

            total_correctness,avg_corr = calc_tracking_attack((retrieve_data_from_json(input_file)), locations, strat)

            if total_correctness == 100:
                break

            df = df.append({'strategy':strat, 'rate':rate, 'iteration':iteration, 'total_correctness':total_correctness,'avg_correctness':avg_corr}, ignore_index=True)
            time1 = datetime.now()

            print("\n one file took:  ",str(time1-time0), " \n\n")
            
        
    df.to_csv(result_file_path) 
    return



if __name__ == '__main__':
    pr = cProfile.Profile()
    pr.enable()
    main()
    pr.disable()
    stats =pstats.Stats(pr)
    stats.sort_stats('tottime').print_stats(7)