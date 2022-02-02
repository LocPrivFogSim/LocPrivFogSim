from helper_methods import *

import os
import matplotlib.pyplot as plt
import pandas as pd



db_con = connect_to_db()

def get_results(path_data,location_for_nodes):
    path_id = path_data[0]
  
    path_coords = get_path_coordinates_from_db(db_con, path_id)
    
    compromised_fog_nodes = path_data[1]

    events = path_data[2]  #event{ fog_device_id, event_name, event_type, event_id, timestamp }

    print(events[0])
    #print(events)

    #find vornoi field for fog_node
    #get all locations inside that voronoi field
    # Propabilities for these locations are equal, all others are 0


    total_correctness = 0

 
    for event in events:
        
        timestamp = event['timestamp']
        correct_pos = get_position_for_timestamp(path_coords, timestamp)

        fog_device_id = event['fog_device_id']
        
        if fog_device_id not in compromised_fog_nodes:  #probability = 0 
            continue

        locations_in_polygon = location_for_nodes[fog_device_id][2]

        probability = 1/len(locations_in_polygon)
        
        location_probabilities = []
        
        for l in locations_in_polygon:
            location_probabilities.append([l,probability])

        #print("loc_probs: ", location_probabilities) 
        #print("fog_device: ",fog_device_id,"    Pos: ",location_for_nodes[fog_device_id][1])
        #print("correct Pos: ", correct_pos)
        #print("locations:   ",locations_in_polygon)
        #print("correctness: ",calc_correctness(location_probabilities, correct_pos))
        #print("-------------------------------")

        
        total_correctness = total_correctness + calc_correctness(location_probabilities, correct_pos)


    avg_corr = total_correctness/len(events)

    #print("####### finished ########")
    #print("total correctness: ",total_correctness)
    #print("avg_correctness: ",avg_corr)
    return total_correctness, avg_corr


def calc_strategy_nearest():

    
    result_file_path = "results/nearest.csv"

    input_json_dir = "input/Strategie_3"
    
    location_for_nodes = retrieve_list_from_json("json/node_locations.json")#[nodeid, node_position,locations]


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
            total_correctness,avg_corr= get_results(retrieve_data_from_json(input_file),location_for_nodes)
            #print(total_correctness)
            #print(avg_corr)
            file_split = (str(file)).split('_') # e.g. ['output', '3', '100', '1.json']
            strat = file_split[1]
            rate = file_split[2]
            iteration = file_split[3].split('.')[0]
            
            df = df.append({'strategy':strat, 'rate':rate, 'iteration':iteration, 'total_correctness':total_correctness,'avg_correctness':avg_corr}, ignore_index=True)

            


    df.to_csv(result_file_path) 


            

    return

def test_voronoi(conn):
    node_positions = select_all_node_positions
    
    vor = Voronoi(node_positions)


    #https://stackoverflow.com/questions/68747267/how-to-link-initial-points-coordinates-to-corresponding-voronoi-vertices-coordin

    print("node[0] = ", node_positions[0])
    print("region for node[0] = ", vor.point_region[0])
    print("vertices for region for node[0] =", vor.regions[vor.point_region[0]])
    print("coordinates of each vertice = \n" , vor.vertices[vor.regions[vor.point_region[0]]])  #todo check if vertices are always ordered the same way


    #fig = voronoi_plot_2d(vor, show_vertices=False, line_colors='orange',line_width=2, line_alpha=0.6, point_size=2)
    fig = voronoi_plot_2d(vor, show_points=False, show_vertices = False,point_size=0)
    plt.show()






def main():
   # db_con= connect_to_db()
    calc_strategy_nearest()



if __name__ == '__main__':
    main()