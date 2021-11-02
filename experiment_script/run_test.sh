#!/bin/bash


rate=1
seed2=12
seed3=5
interval=0.1

time_begin=0
time_before=0
time_after=0
time_needed=0

#java -jar MobFogSim_privacy.jar 3 0.5 44 5 0.01




echo -e "\npossible paths, average path length, area (geo), area(num), g_1(geo), g_1(num), g_2(geo), g_2(num)" >> results/results.csv

time_begin=$(date +%s)


for ((scenario=3;scenario<=4;scenario++))
do	
	time_before=$(date +%s)
	
	
	echo -e "\nScenario "$scenario",	rate of compromised devices: "$rate"\n" >> results/results.csv
	for ((i=0;i<10;i++))
	do
		let seed2=$RANDOM%100
		let seed3=$RANDOM%20
		java -jar MobFogSim_privacy.jar $scenario $rate $seed2 $seed3 $interval
	done
	
	time_after=$(date +%s)
	let time_needed=$time_after-$time_before
	echo -e "time needed: "$time_needed" seconds\n\n" >> results/results.csv	
	
done

let time_needed=$time_after-$time_begin
echo -e "total time: "$time_needed" seconds\n" >> results/results.csv	











