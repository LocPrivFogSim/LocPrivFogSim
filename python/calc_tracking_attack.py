from datetime import datetime
from turtle import distance, position
from helper_methods import *
import pandas as pd
import os
from math import sqrt
import numpy as np

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
    result_file_path = "results/not_slow.csv"

    input_json_dir = "input/Strategie_1"

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
            total_correctness,avg_corr= calc_strategy_fastest(retrieve_data_from_json(input_file),locations)
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