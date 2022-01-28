from helper_methods import *

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