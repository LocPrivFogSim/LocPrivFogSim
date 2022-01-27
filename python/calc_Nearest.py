from helper_methods import *



def calc_strategy_nearest(events:list, locations:list, conn):
    cursor = conn.cursor()
    cursor.execute("SELECT lat, lon FROM node_positions")
    node_positions = cursor.fetchall()

    return

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
   

    calc_strategy_nearest()



if __name__ == '__main__':
    main()