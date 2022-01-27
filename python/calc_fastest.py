from helper_methods import *




def calc_strategy_fastest(events:list, locations:list, fog_nodes:list ):
    
    #for each location check which fog node is the fastest to respond

    current_min = 1000000000 #some high nr
    
    

    for event in events:
        fog_device_id = event[0]
        for location in locations:
            response_time =  calc_response_time
            if(response_time < current_min):
                current_min =  response_time



def main():
    return



if __name__ == '__main__':
    main()