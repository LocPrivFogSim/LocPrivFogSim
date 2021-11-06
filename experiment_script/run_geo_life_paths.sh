#!/bin/bash

rate=0.05
seed2=12
seed3=5
can_be_turned_off=0
interval=0.1
paths_per_go=70


time_begin=0
time_before=0
time_after=0
time_needed=0



time_begin=$(date +%s)



for ((scenario=1;scenario<=4;scenario++))
do

	time_before=$(date +%s)

	for ((i=5;i<=100;i=$i+5))
	do	
		
		for ((j=0;j<15;j++))
		do
			let seed2=$RANDOM%50+1
			let seed3=$RANDOM%20
			java -jar LocPrivFogSim.jar $scenario $i $seed2 $seed3 $paths_per_go
		done
	done

	time_after=$(date +%s)
	let time_needed=$time_after-$time_before
	echo -e "time needed: "$time_needed" seconds\n\n" >> results/results.csv

done




let time_needed=$time_after-$time_begin
echo -e "total time: "$time_needed" seconds\n" >> results/results.csv

echo -e "Fertig" >> results/results.csv
