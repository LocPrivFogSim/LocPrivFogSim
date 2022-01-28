from helper_methods import *

import matplotlib.pyplot as plt




db_con = connect_to_db()


def calc_strategy_nearest():

    time1 = datetime.now


    path_data = retrieve_data_from_json("output_12_1_1.json") #TODO loop through all files later
    path_id = path_data[0]
  
    path_coords = get_path_coordinates_from_db(db_con, path_id)
    
    compromised_fog_nodes = path_data[1]


    events = path_data[2]  #event{ fog_device_id, event_name, event_type, event_id, timestamp }


    #find vornoi field for fog_node
    #get all locations inside that voronoi field
    # Propabilities for these locations are equal, all others are 0


    total_correctness = 0

    counter = 0
    for event in events:
        counter = counter + 1
        print(counter," von" , len(events))

        timestamp = event['timestamp']
        correct_pos = get_position_for_timestamp(path_coords, timestamp)

        #TODO read json for location probabilities

        probability = 1/len(locations_in_polygon)
        
        location_probabilities = []
        
        for l in locations_in_polygon:
            location_probabilities.append([l,probability])
        
        total_correctness = total_correctness + calc_correctness(location_probabilities, correct_pos)


    avg_corr = total_correctness/len(events)
    print(total_correctness)
    print(avg_corr)
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