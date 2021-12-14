import sqlite3
import json
import matplotlib.pyplot as plt
from scipy.spatial import Voronoi, voronoi_plot_2d

json_path = "../privacy/output.json"
db_path = "../geoLifePaths.db"


def connect_to_db(path):
    conn = None
    try:
        conn = sqlite3.connect(path)
    except Error as e:
        print(e)
    return conn


def select_path_from_db(conn, path_id):
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM paths WHERE path_id == " + str(path_id))

    rows = cursor.fetchall()
    
    if(len(rows) != 1):
        raise ValueError( str(len(rows)) + ' paths (!=1) selected! in metricsCalculator.select_path_from_db()') 

    path = rows[0]
    return path



def trace_comp_set(conn, path, observed_order, compromised_fog_nodes):
    """
    returns a list of all paths that match the adversarys observations
    """
    

    cursor = conn.cursor()
    cursor.execute("SELECT path_id, duplicates, fog_nodes_trace FROM paths")
    
    #path_id, duplicates, fog_nodes_trace 
    paths = cursor.fetchall()

    comp_set = []

    observed_order = [ int(x) for x in observed_order] #parse int

    for path in paths:
        order_of_fognodes = path[2] #[2] fog_node_trace
        order_of_fognodes = order_of_fognodes.split(',')
        order_of_fognodes = [ int(x) for x in order_of_fognodes] #parse int
        #now remove all uncompromised fog_nodes and remove consecutive duplicates  
        order_of_fognodes = [ x for i, x in enumerate(order_of_fognodes) if (i == 0 and x in compromised_fog_nodes) or (x != order_of_fognodes[i-1]  and x in compromised_fog_nodes)]       
        
        if(len(order_of_fognodes) !=0):
            if int(observed_order[0]) == int(order_of_fognodes[0]):
                print(observed_order)
                print(order_of_fognodes)

        if observed_order == order_of_fognodes:
            comp_set.append(path)
    
    print (comp_set)

    return comp_set

    
def get_observed_order_of_fognodes(events):
    """
    eg. fog_device_ids from one example json:
    [1100, 1100, 1100, 999, 999, 200, 300] -> returns [1100, 999, 200, 300]
    """
    
    order = []
    
    for i in range(len(events)):
        fog_device_id = events[i]['fog_device_id']

        if(len(order) == 0):
            order.append(fog_device_id)
            continue

        if(order[len(order) -1 ]   != fog_device_id):
            order.append(fog_device_id)

    if(len(order) == 0):
        raise ValueError( 'order is empty! in metricsCalculator.observed_order_of_fognodes()') 

    return order


def test_voronoi(conn):
    cursor = conn.cursor()
    cursor.execute("SELECT lat, lon FROM node_positions")
    node_positions = cursor.fetchall()
    
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

    conn = connect_to_db(db_path)
    test_voronoi(conn)
    

    exit()

    with open(json_path) as json_file:
        data = json.load(json_file)
    
    path_id = data['simulatedPath']
    simulatedScenario = data['simulatedScenario']
    compromised_fog_nodes = data['compromisedFogNodes']
    events = data['events']     #each event { fog_device_id, event_name, event_type, event_id, timestamp }
    
    observed_order_of_fognodes = get_observed_order_of_fognodes(events)
   
    #path_id | path | duplicates | distance | min_lat | max_lat| minlon | max_lon | fog_nodes_trace
    path = select_path_from_db(conn, path_id)

    trace_comp_set(conn, path, observed_order_of_fognodes, compromised_fog_nodes)


 


if __name__ == '__main__':
    main()