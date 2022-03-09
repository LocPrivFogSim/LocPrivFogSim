from datetime import datetime
from turtle import distance, position
from helper_methods import *
import pandas as pd
import os
from math import sqrt
import numpy as np

import multiprocessing as mp

import cProfile
import pstats


db_con = connect_to_db()

df = pd.DataFrame(columns=['strategy','rate','iteration','total_correctness','avg_correctness'])

results = []

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

rel_locations_for_node = {} 


@njit
def calc_response_time(in_data_size, out_data_size, mi, position, up_bandwidth, down_bandwidth, mips, sample_point):
    
    #position = numpy.array([numpy.float64(position[0]), numpy.float64(position[1])])
    distance = calc_dist_njit(position, sample_point)
    distance_factor = 1 - (distance / max_distance)
    up_transfere_time = in_data_size / (up_bandwidth * distance_factor)
    calculation_time = mi / mips
    down_transfere_time = out_data_size / (down_bandwidth * distance_factor)
    return up_transfere_time + calculation_time + down_transfere_time

def calc_not_slow(strat, rate, iteration, path_data, locations:list):

    #path_data [path_id, compromised_fog_nodes, events, fog_device_infos, device_stats]
    #fog_device_infos [fog_device_id, downlink_bandwidth, uplink_bandwidth, uplink_latency]
    #event [fog_device_id, event_name, event_type, event_id, timestamp, availableMips, taskID, dataSize, mi, maxMips]
    #device_stats []
    locations = np.array([np.array([l[0], l[1]]) for l in locations])



    path_id = path_data[0]

    path_coords = get_path_coordinates_from_db(db_con, path_id)


    compromised_fog_nodes = path_data[1]
    events = path_data[2]  #event{ fog_device_id, event_name, event_type, event_id, timestamp, consideredFogNodes, consideredField, ...}
    
    fog_device_infos = path_data[3]
    fog_device_infos = trans_device_infos(fog_device_infos)
    device_stats = path_data[4]


    fog_device_positions = select_all_node_positions(db_con)
    fog_device_positions = np.array(fog_device_positions)


    total_correctness = 0
    avg_corr = 0

    
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
        
         #if relev 
        if fog_device_id not in rel_locations_for_node.keys():
            relevant_locations = get_relevant_locations(locations, selected_fog_node_position, edges)
            rel_locations_for_node[fog_device_id] = relevant_locations

        relevant_locations = rel_locations_for_node[fog_device_id]

        


        #print("selected_fn_position: ",selected_fog_node_position)
        #print("edgepoints: ",edges)
        #print("example relevant pos: ",relevant_locations[0])
        #print("eventId: ", event['event_id'])
        
        considered_fog_devices = np.array(event['consideredFogNodes'])
        
        prob_location= 1/len(relevant_locations)        #Pr(ℓ) = 1/|L|
        prob_fog_node = 1/len(considered_fog_devices)   #Pr(f∗) = 1/|F|

        threshold_distr = {0.03:0.25,
        0.0462: 0.5,
        0.5: 0.25}  #key=threshold, val=prob
        
        location_prob_dict = {}

        for location in relevant_locations:
            prob = 0
           
            for threshold in threshold_distr.keys():
                cond_probability_node = cond_prob_select_node(location, add_event, remove_event, fog_device_infos, device_stats, fog_device_positions, considered_fog_devices, fog_device_id, threshold)              
                prob_threshold = threshold_distr[threshold]

                prob += cond_probability_node * prob_threshold #

                #print("cond_prob: ", cond_probability_node)


            prob = (prob_location/prob_fog_node) * prob
            if prob > 0:
                location_prob_dict[(location[0], location[1])] = prob     

        for location in location_prob_dict.keys():
            distance = calc_dist_in_m(location, actual_position)
            #print(distance)
            correctness = location_prob_dict[location] * distance
            total_correctness += correctness

    

    if(counted_events == 0):
        return (strat, rate, iteration, 0, 0)     
    
    avg_corr = total_correctness/counted_events

    #print ("avg_corr: ",avg_corr, "      total: ",total_correctness)

    return (strat, rate, iteration, total_correctness, avg_corr)


#Pr(f∗ |ℓ,x)  = 1/|ˆF(ℓ,x)| if f∗ ∈ ˆF(ℓ,x   || 0 otherwise 
#conditional prob that node f* is selected from location l for threshold x
def cond_prob_select_node(location, add_event, remove_event, fog_device_infos, device_stats, fog_device_positions, considered_fog_devices,selected_fog_node_id, threshold):
    in_data_size = add_event['dataSize']
    out_data_size = remove_event['dataSize']
    mi = add_event['mi']
    sample_point = location
    base_mips = add_event['maxMips']
    task_id = add_event['taskId']
    id_with_min_mips = list(device_stats[task_id].keys())[0]
    min_mips = device_stats[task_id][id_with_min_mips]

    not_slow = find_not_slow_loop(considered_fog_devices, base_mips, fog_device_positions, fog_device_infos,  id_with_min_mips, min_mips, in_data_size,out_data_size, mi, sample_point, threshold )

    if len(not_slow) == 0:
        return 0

    #print(len(not_slow), "  len not slow")
    if selected_fog_node_id not in not_slow:
        return 0
    
    return 1/len(not_slow)

@njit
def find_not_slow_loop(considered_fog_devices, base_mips, fog_device_positions, fog_device_infos, id_with_min_mips , min_mips, in_data_size, out_data_size, mi, sample_point, threshold ):
  

    arr = np.zeros(len(considered_fog_devices))
    j = 0
  
    for i in range(len(considered_fog_devices)): 
        current_id = considered_fog_devices[i]

        mips = base_mips
        position = fog_device_positions[current_id]
        #position = numpy.array([numpy.float64(position[0]), numpy.float64(position[1])])
        device = fog_device_infos[current_id]       

        down_bandwidth = device[0]
        up_bandwidth = device[1]

        if current_id == id_with_min_mips:
            mips = min_mips
        
        response_time = calc_response_time(in_data_size, out_data_size, mi, position, up_bandwidth, down_bandwidth, mips, sample_point)
        
        if response_time < threshold:
            arr[j] = current_id
            j += 1
 
    arr = arr[0:j]
       
    return arr

def main():
    result_file_path = "results/not_slow.csv"

    input_json_dir = "input/Strategie_1"
    #input_json_dir = "input/Test"


    locations= retrieve_list_from_json("json/locations_points.json")#[nodeid, node_position,locations]

     #clear result file
    f =  open(result_file_path, 'w+')
    f.close

    df = pd.DataFrame(columns=['strategy','rate','iteration','total_correctness','avg_correctness'])

    pool = mp.Pool(mp.cpu_count())
    #pool = mp.Pool(1)
    
     #iterate input files
    for dirpath, dirs, files in os.walk(input_json_dir):
        for file in files:
            time0 = datetime.now()
            print(dirpath, "     - file: ",file)
            input_file = os.path.join(dirpath,file)
            file_split = (str(file)).split('_') # e.g. ['output', '3', '100', '1.json']
            strat = file_split[1]
            rate = file_split[2]
            iteration = file_split[3].split('.')[0]

            #total_correctness,avg_corr= calc_not_slow(retrieve_data_from_json(input_file),locations)
            pool.apply_async(calc_not_slow, args=(strat, rate, iteration, retrieve_data_from_json(input_file), locations), callback=append_results)

            #strat, rate, iteration, path_data, locations:list

            #df = df.append({'strategy':strat, 'rate':rate, 'iteration':iteration, 'total_correctness':total_correctness,'avg_correctness':avg_corr}, ignore_index=True)
            time1 = datetime.now()

            print("\n one file took:  ",str(time1-time0), " \n\n")
    #print("LOOP DONE")        
    pool.close()
    pool.join()        
    
    #print("pool closed")
    #print(results)
    #print(df)
    for result in results:
        strat = result[0]
        rate = result[1]
        iteration = result[2]
        total_correctness = result[3]
        avg_corr = result[4]
        if(total_correctness > 0):
            df = df.append({'strategy':strat, 'rate':rate, 'iteration':iteration, 'total_correctness':total_correctness,'avg_correctness':avg_corr}, ignore_index=True)


    df.to_csv(result_file_path) 
    return


def append_results(result):#
    #result [strat, rate, iteration, total_corr, avg_corr]
    strat = result[0]
    rate = result[1]
    iteration = result[2]
    total_correctness = result[3]
    avg_corr = result[4]


    results.append([strat, rate, iteration, total_correctness, avg_corr])

    #global df 
    #df.append({'strategy':strat, 'rate':rate, 'iteration':iteration, 'total_correctness':total_correctness,'avg_correctness':avg_corr}, ignore_index=True)
    

if __name__ == '__main__':
    pr = cProfile.Profile()
    pr.enable()
    main()
    pr.disable()
    stats =pstats.Stats(pr)
    stats.sort_stats('tottime').print_stats(10)