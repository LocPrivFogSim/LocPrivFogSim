#!/bin/bash

#rate=0.05
#seed2=12
#seed3=5
can_be_turned_off=0
interval=5
paths_per_go=70
offloading_threshold=0.0462
strat=""

time_begin=0
time_before=0
time_after=0
time_needed=0



time_begin=$(date +%s)



for ((scenario=1;scenario<=2;scenario++))
do

	time_before=$(date +%s)

	for ((i=5;i<=100;i=$i+$interval)) #i is rate of compromised devices
	do
		for((j=1;j<=3;j++))			#j is offloading_strat  1 = BelowThresholdRandomDevice, 2 = BelowThresholdLowestResponseTime, 3 = ClosestFogDevice

		do
			let seed2=$RANDOM%50+1
			let seed3=$RANDOM%20
			java -jar LocPrivFogSim.jar $scenario ($i/100) $seed2 $seed3 $offloading_threshold $j
		
	done

	time_after=$(date +%s)
	let time_needed=$time_after-$time_before
	echo -e "time needed: "$time_needed" seconds\n\n" >> results/results.csv

done




let time_needed=$time_after-$time_begin
echo -e "total time: "$time_needed" seconds\n" >> results/results.csv

echo -e "Fertig" >> results/results.csv
