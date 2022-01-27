#!/bin/bash

rate_start=30
rate_end=40
rate_interval=10

offloading_strategy_start=1
offloading_strategy_end=3
offloading_threshold=0.0462

iteration_start=1
iterations=100

enable_logging="false"

# misc variables
time_script_start=0
time_script_end=0
time_rate_start=0
time_rate_end=0
time_strategy_start=0
time_strategy_end=0

# start of script execution...
time_script_start=$(date +%s) # start time of script execution

# Loop through the rate of compromised nodes in $rate_interval steps
# Currently we start with 5% of compromised fog nodes and use an interval of 5 percent points up to a rate of 100%
# compromised fog nodes.
for ((i=$rate_start;i<=$rate_end;i+=$rate_interval)) # i is the rate of compromised nodes
do

  time_rate_start=$(date +%s) # start time for the current selected rate of compromised fog nodes

  # Loop through the different offloading strategies we have:
  # - j = 1: BelowThresholdRandomDevice
  # - j = 2: BelowThresholdLowestResponseTime
  # - j = 3: ClosestFogDevice
  for((j=$offloading_strategy_start;j<=$offloading_strategy_end;j++)) # j is the offloading strategy used
  do

    time_strategy_start=$(date +%s) # start time for the current selected offloading strategy

    for((k=$iteration_start;k<=$iterations;k++)) # k is the number of iterations the simulator is executed
    do
      # execute simulator
      let seed2=$RANDOM%50+1
      let seed3=$RANDOM%20
      java -jar LocPrivFogSim.jar $i $seed2 $seed3 $offloading_threshold $j $enable_logging $k
    done

    time_strategy_end=$(date +%s) # end time for the current selected offloading strategy
    time=($time_strategy_end-$time_strategy_start)
    echo -e "Simulation of scenario "$scenario" with rate of "$i" for offloading strategy "$j" took "$time" seconds\r\n" >> results/results.csv
  done

  time_rate_end=$(date +%s) # end time for the current selected rate of compromised fog nodes
  time=($time_rate_end-$time_rate_start)
  echo -e "Simulation of all offloading strategies with rate of "$i" for scenario "$scenario" took "$time" seconds\r\n" >> results/results.csv
done

time_script_end=$(date +%s) # end time of script execution
time=($time_script_end-$time_script_start)
echo -e "Total script execution time: "$time" seconds\n" >> results/results.csv
echo -e "Script finished" >> results/results.csv
