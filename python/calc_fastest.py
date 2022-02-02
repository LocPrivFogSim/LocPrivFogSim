from helper_methods import *
import pandas as pd
import os

db_con = connect_to_db()

# in_data_size    => Tasks input data size
# out_data_size   => Tasks output data size
# mi              => Tasks mi
# up_bandwidth    => Target fog node upload bandwidth
# down_bandwidth  => Target fog node download bandwidth
# mips            => Target fog node available mips at time t
def calc_response_time(in_data_size, out_data_size, mi, up_bandwidth, down_bandwidth, mips):
    up_transfere_time = in_data_size / up_bandwidth
    calculation_time = mi / mips
    down_transfere_time = out_data_size / down_bandwidth

    return up_transfere_time + calculation_time + down_transfere_time



def calc_strategy_fastest(path_data, locations:list):

    path_id = path_data[0]
  
    path_coords = get_path_coordinates_from_db(db_con, path_id)
    
    compromised_fog_nodes = path_data[1]

    events = path_data[2]  #event{ fog_device_id, event_name, event_type, event_id, timestamp }

    fog_device_infos = path_data[3]

    device_stats = path_data[4]

    print(device_stats.keys())
  

    total_correctness = 0
    avg_corr = 0

    #for each location check which fog node is the fastest to respond    
    current_min = 1000000000 #some high nr



    for event in events:
        fog_device_id = event[0]
        for location in locations:
            response_time =  calc_response_time
            if(response_time < current_min):
                current_min =  response_time

    return total_correctness, avg_corr



def main():
    result_file_path = "results/fastest.csv"

    input_json_dir = "input/Strategie_2"

    locations= retrieve_list_from_json("json/locations_points.json")#[nodeid, node_position,locations]

     #clear result file
    f =  open(result_file_path, 'w+')
    f.close

    df = pd.DataFrame(columns=['strategy','rate','iteration','total_correctness','avg_correctness'])

     #iterate input files
    for dirpath, dirs, files in os.walk(input_json_dir):
        for file in files:
            print(dirpath)
            print(file)
            input_file = os.path.join(dirpath,file)
            total_correctness,avg_corr= calc_strategy_fastest(retrieve_data_from_json(input_file),locations)
            file_split = (str(file)).split('_') # e.g. ['output', '3', '100', '1.json']
            strat = file_split[1]
            rate = file_split[2]
            iteration = file_split[3].split('.')[0]
            
            df = df.append({'strategy':strat, 'rate':rate, 'iteration':iteration, 'total_correctness':total_correctness,'avg_correctness':avg_corr}, ignore_index=True)


    return



if __name__ == '__main__':
    main()