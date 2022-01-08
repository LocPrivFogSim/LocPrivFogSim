#!/bin/bash

interval=5
offloading_threshold=0.0462

# misc variables
time_script_start=0
time_script_end=0
time_scenario_start=0
time_scenario_end=0
time_rate_start=0
time_rate_end=0
time_strategy_start=0
time_strategy_end=0

# start of script execution...
time_script_start=$(date +%s) # start time of script execution

# The scenario describes the difference in the adversary's knowledge about the locations of all fog nodes:
# - Scenario 1 = The adversary knows the locations of the compromised fog nodes only.
# - Scenario 2 = The adversary knows the locations of all fog nodes.
for ((scenario=1;scenario<=2;scenario++))
do

	time_scenario_start=$(date +%s) # start time for the current scenario

  # Loop through the rate of compromised nodes in $interval steps
  # Currently we start with 5% of compromised fog nodes and use an interval of 5 percent points up to a rate of 100%
  # compromised fog nodes.
	for ((i=5;i<=100;i=$i+$interval)) # i is the rate of compromised nodes
	do

	  time_rate_start=$(date +%s) # start time for the current selected rate of compromised fog nodes

	  # Loop through the different offloading strategies we have:
	  # - j = 1: BelowThresholdRandomDevice
	  # - j = 2: BelowThresholdLowestResponseTime
	  # - j = 3: ClosestFogDevice
		for((j=1;j<=3;j++)) # j is the offloading strategy used
		do

		  time_strategy_start=$(date +%s) # start time for the current selected offloading strategy

			let seed2=$RANDOM%50+1
			let seed3=$RANDOM%20

			java -jar LocPrivFogSim.jar $scenario $i $seed2 $seed3 $offloading_threshold $j

			time_strategy_end=$(date +%s) # end time for the current selected offloading strategy
			echo -e "Simulation of scenario "$scenario" with rate of "$i" for offloading strategy "$j" took "(($time_strategy_end-$time_strategy_start))" seconds\r\n" >> results/results.csv
	  done

    time_rate_end=$(date +%s) # end time for the current selected rate of compromised fog nodes
    echo -e "Simulation of all offloading strategies with rate of "$i" for scenario "$scenario" took "((time_rate_end-time_rate_start))" seconds\r\n" >> results/results.csv
  done

  time_scenario_end=$(date +%s) # end time for the current scenario
  echo -e "Simulation of all offloading strategies with all rate with an interval of "$interval" for scenario "$scenario" took "((time_scenario_end-time_scenario_start))" seconds\r\n" >> results/results.csv
done

time_script_end=$(date +%s) # end time of script execution
echo -e "Total script execution time: "((time_script_end-time_script_start))" seconds\n" >> results/results.csv
echo -e "Script finished" >> results/results.csv
