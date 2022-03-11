from array import array
from datetime import datetime, timedelta
import pstats
from random import randrange
import traceback
from turtle import pos
from shared_methods import *
#from calc_fastest import get_fastest_comp_fog_node
import pandas as pd
import os
#from math import sqrt
import numpy as np
import math

from numba import njit, jit
from numba.core import types
from numba.typed import Dict

import multiprocessing as mp

import cProfile



#Arraytype for numba
float64_array = types.float64[:]
float32_array = types.float32[:]


db_con = connect_to_db()

rel_locations_for_node = {} 

location_for_nodes = retrieve_list_from_json("json/node_locations.json")

nr_of_paths = 57947
paths_global = {}
fog_device_positions_global = []

def set_globals():
    global paths_global
    for i in range(nr_of_paths+1):
        path = select_path_from_db(db_con, i)
        path_dict = path_as_dict(path)
        paths_global[i] = path_dict
    fog_device_positions = select_all_node_positions(db_con)
    global fog_device_positions_global
    fog_device_positions_global = np.array(fog_device_positions)

def calc_tracking_attack(path_data, locations, strat, rate, iteration):
    #path_data [path_id, compromised_fog_nodes, events, fog_device_infos, device_stats]
    
    locations = np.array(locations)

    actual_path_id = path_data[0]

    #print(actual_path_id)

    #actual_coords = get_path_coordinates_from_db(db_con, actual_path_id)
    #actual_coords_dict = get_coords_dict(actual_coords)
    actual_coords = paths_global[actual_path_id]['path_coords']
    actual_coords_dict = get_coords_dict(actual_coords)

    compromised_fog_nodes = path_data[1]
    events = path_data[2]
    fog_device_infos = path_data[3]

    fog_device_infos = trans_device_infos(fog_device_infos)   
    
    device_stats = path_data[4]

    # remove all events that dont belong to a compromised fog_node
    events = [event for event in events if event['fog_device_id'] in compromised_fog_nodes]
    
    if len(events) == 0:
        return (strat, rate, iteration, 0, 0, 0) 
    
    tracked_duration = events[-1]['timestamp']-events[0]['timestamp'] #TODO (last tracked_fog_nodes[timestamp] - first )
    number_of_observations = len(events)  #k

    debug_path_heuristik = 0 #TODO remove


    path_prob = {}

    #for path_id in range(36196,36197):
    for path_id in range(0, nr_of_paths):

        #path = select_path_from_db(db_con, path_id)
        #path = path_as_dict(path)
        #path = paths_global[path_id]

        alpha = 0

        path = paths_global[path_id]
        
        #print(path_coords)
        
        path_coords = path['path_coords']
        path_coords = np.array([[p[0],p[1]] for p in path_coords ]  )

        if calc_dist_in_m(path_coords[0], actual_coords[0]) > 2000: #heuristic to reduce runtime
            continue
        
        debug_path_heuristik += 1

        print("path nr: ", path_id)

        len_of_segments = 25 # in metres
        nr_of_segments = math.ceil(path['distance']/len_of_segments)
        
        segments = divide_path_into_segments(path_coords, len_of_segments, nr_of_segments)  #divide path into path into segments P1... Pc


        #x_debug = [[x[2], x[3]]  for x in segments]
        #y_debug = actual_coords

        #f1 = open('segments.gpx', "w")
        #f1.write(createGPX(x_debug))
        #f1.close

        #f2 = open('original.gpx', "w")
        #f2.write(createGPX(y_debug))
        #f2.close

                        
        nr_iterations = 50 #TODO probably increase

        for j in range(nr_iterations):
            
            
            segments = sample_velocities(segments)
            time_for_traversing = sum([ segment[6] for segment in segments])
            
            if time_for_traversing > tracked_duration:
                beta = 1
                               
                max_t0 = int(time_for_traversing - tracked_duration)
                rand_t0 = 0
                if max_t0 > 0:
                    randrange(0,max_t0)
                
                #rand_t0 = 0

                segments_dict = get_segments_dict(segments, rand_t0, tracked_duration) 

                timestamps = [event['timestamp'] for event in events]
                timestamps = np.array(list(dict.fromkeys(timestamps)))  #remove duplicates

                try:
                    locations_on_path = get_locations_at_ts(segments_dict, timestamps)
                except Exception as e: 
                    print("duration   ", tracked_duration)
                    print(timestamps)
                    print(segments_dict) 
                    print(traceback.format_exc())
                    exit()

                for i in range(int(number_of_observations/2)):
                    e = events[i*2]

                    selected_fog_node = e['fog_device_id']

                    try:
                        timestamp = e["timestamp"]
                        guessed_location = locations_on_path[timestamp]                 

                    except Exception:
                        print("Error while getting the guessed location")
                        print(timestamps)
                        print(locations_on_path)
                        print(segments_dict)
                        exit()


                    actual_position = actual_coords_dict[timestamp]
                    #actual_position = np.array([float(actual_position[0]), float(actual_position[1])])
                    
                                
                    strategy_id = int(strat)

                    prob_for_location = 0
                   
                    # - strat = 1: BelowThresholdRandomDevice
                    # - strat = 2: BelowThresholdLowestResponseTime
                    # - strat = 3: ClosestFogDevice
                    if strategy_id ==1 :
                        prob_for_location = prob_not_slow(guessed_location, actual_position, e, events[i+1], fog_device_infos, device_stats, fog_device_positions_global, locations)
                        #if prob_for_location > 0:
                        #    print (prob_for_location)
                    
                    if strategy_id ==2 :
                        prob_for_location = prob_fastest(guessed_location, actual_position, e, events[i+1], fog_device_infos, device_stats, fog_device_positions_global, locations)

                    if strategy_id ==3 :
                        prob_for_location = prob_clostest(guessed_location, selected_fog_node)
                    
                                        
                    if prob_for_location > 0:
                        beta = beta + prob_for_location
                    
                alpha += beta

        if alpha > 0:
            path_prob[path_id] = alpha


    #print("Anzahl untersuchter Pfade: ",debug_path_heuristik)
    #print("\n\n DONEEEE")


    corr_full_dtw_distance = 0
    corr_avg_dtw_distance = 0


    #print(path_prob)

    #exit()

    total_alphas = sum(path_prob.values())


    for path_id in path_prob.keys():
        
        prob_path = path_prob[path_id]/total_alphas

        path_coords =paths_global[path_id]['path_coords']
        path_coords = np.array([[float(coord[0]), float(coord[1])]  for coord in  path_coords])

        actual_coords = np.array([[float(coord[0]), float(coord[1])]  for coord in  actual_coords])
        distance, dtw_matrix = dtw_njit(path_coords, actual_coords)
        
        warping_path = compute_optimal_warping_path(dtw_matrix)
        
        corr_full_dtw_distance += prob_path * distance 
        corr_avg_dtw_distance += prob_path * (distance / len(warping_path))

        #x_debug = path_coords
        #y_debug = actual_coords
        #f1 = open('guessed_path.gpx', "w")
        #f1.write(createGPX(x_debug))
        #f1.close
        #f2 = open('original_path.gpx', "w")
        #f2.write(createGPX(y_debug))
        #f2.close
        #print("path id: ", path_id)
        #print("actual path: ", actual_path_id)
        #print("dtw: ",distance)
        #print("dtw pairs: ",  len(warping_path) ,"           :", warping_path)
        #print("avg dtw: ", distance/len(warping_path))
        #print("prob for path:  ",prob_path)
        #print("################")
        #print("guessed_coords  ", x_debug)
        #print("/n actual path: ", y_debug)
        #exit()

    
    return (strat, rate, iteration, corr_full_dtw_distance, corr_avg_dtw_distance, number_of_observations) 


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
        velocity = (randrange(4,10)*1000)/(60*60)
        segment[5] = velocity        # velocity TODO
        segment[6] = segment[4]/velocity   # traversing_time
    return segments

@njit
def get_segments_dict(segments, t_0, duration):

    segments_map = Dict.empty(
        key_type=types.float64,
        value_type=float64_array
    )


    sum_times = 0
    first_elem = 0


    for i in  range(len(segments)):
        sum_times += segments[i][6]
        if(sum_times < t_0):
            first_elem = i
            continue
        break
    
    sum_times = 0
    
    for j in range(first_elem,len(segments)):
        segments_map[sum_times] = segments[j]
        
        if (sum_times > duration):
            break
        sum_times += segments[j][6]

    return segments_map

@njit
def get_locations_at_ts(segments_dict, timestamps):

    locations_dict = Dict.empty(
        key_type=types.int64,
        value_type=float64_array
    )

    keys = list(segments_dict.keys())     

    i = 0

    for j in range(len(timestamps)):
        if i >= len(keys):
                break 
       
        while not keys[i] > timestamps[j] and i < len(keys)-1: #timestamp relates to point on current segment
            i+=1
            

        timestamp = timestamps[j]
        key = keys[i-1]
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
  
    considered_fog_devices = np.array(current_add_event['consideredFogNodes'])
    
    prob_location= 1/len(relevant_locations)        #Pr(ℓ) = 1/|L|
    prob_fog_node = 1/len(considered_fog_devices)   #Pr(f∗) = 1/|F|

    threshold_distr = {0.03:0.25,
        0.0462: 0.5,
        0.5: 0.25}  #key=threshold, val=prob

    prob_total = 0
    
    for threshold in threshold_distr.keys():
        
        prob_guessed_location =  cond_prob_guessed_location(guessed_location, current_add_event, next_remove_event, fog_device_infos, device_stats, fog_device_positions, considered_fog_devices, actual_device, threshold)

        prob_threshold = threshold_distr[threshold]

        prob_total += prob_guessed_location*prob_threshold

    #print ((prob_location/prob_fog_node) * prob_total)
    return (prob_location/prob_fog_node) * prob_total

   
def prob_fastest(guessed_location, actual_position, current_add_event, next_remove_event, fog_device_infos, device_stats, fog_device_positions, locations):

    if calc_dist_njit(guessed_location, actual_position) > 10000 * sqrt(2):
        return 0

    actual_device = current_add_event['fog_device_id']
    try:
        actual_device_position = fog_device_positions_global[actual_device]
    except:
        print("id: ", actual_device)
        print(len(fog_device_positions_global))
        exit()
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
  
    considered_fog_devices = np.array(current_add_event['consideredFogNodes'])
    

    chosen_device = get_fastest_comp_fog_node(guessed_location, current_add_event, next_remove_event, fog_device_infos, device_stats, fog_device_positions, considered_fog_devices)
    

    if chosen_device != actual_device:
        return 0

    #print("'!!  chosen  == actual device !!")

    possible_locations = []

    for location in relevant_locations:
        chosen_device = get_fastest_comp_fog_node(location, current_add_event, next_remove_event, fog_device_infos, device_stats, fog_device_positions, considered_fog_devices)
        if chosen_device == actual_device:
            possible_locations.append(location)
    

    #print("weitere mögliche Positionen: ",possible_locations)

    return 1/(len(possible_locations) + 1)


def prob_clostest(guessed_location, device_id):
    locations_in_polygon = location_for_nodes[device_id][2]
    vertices = location_for_nodes[device_id][3]
    vertices = [ np.array([v[0],v[1]]) for v in vertices]
    vertices = np.array(vertices)

    is_in = ray_tracing(guessed_location[0], guessed_location[1], vertices)

    if is_in == True:
        #print("is in for   ", guessed_location, "    vertices", vertices)

        return 1 / (len(locations_in_polygon) +1)
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

   # input_json_dir = "input/Strategie_3"    #Todo

    input_json_dir = "input/Test"
   
    set_globals()

    print("globals set!")

    locations= retrieve_list_from_json("json/locations_points.json")#[nodeid, node_position,locations]
    location_for_nodes = retrieve_list_from_json("json/node_locations.json")#[nodeid, node_position,locations, voronoi_vertices]    

     #clear result file
    f =  open(result_file_path, 'w+')
    f.close

    df = pd.DataFrame(columns=['strategy','rate','iteration','corr_full_dtw_distance','corr_avg_dtw_distance','number_of_observations'])

    #pool = mp.Pool(mp.cpu_count())
    pool = mp.Pool(1)

    c = 0

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

            strat, rate, iteration, corr_full_dtw_distance, corr_avg_dtw_distance, number_of_observations  = calc_tracking_attack(retrieve_data_from_json(input_file), locations, strat, rate, iteration)
            #pool.apply_async(calc_tracking_attack, args=(retrieve_data_from_json(input_file), locations, strat, rate, iteration), callback=get_results)
            
    
            #if total_correctness == 100:
            #    break

            df = df.append({'strategy':strat, 'rate':rate, 'iteration':iteration, 'corr_full_dtw_distance':corr_full_dtw_distance,'corr_avg_dtw_distance':corr_avg_dtw_distance,'number_of_observations':number_of_observations}, ignore_index=True)
            time1 = datetime.now()

            print("\n one file took:  ",str(time1-time0), " \n\n")
            
            if corr_avg_dtw_distance > 0:
                c += 1
                print(" c + 1")
                if c > 5:
                    break
                

    pool.close()
    pool.join()    

    df.to_csv(result_file_path)
    #print(results1)

    return


def get_results(result):
    global results1 
    results1[result[0]] = result[1]

results1 = {}

if __name__ == '__main__':
    pr = cProfile.Profile()
    pr.enable()
    main()
    pr.disable()
    stats =pstats.Stats(pr)
    stats.sort_stats('tottime').print_stats(10)