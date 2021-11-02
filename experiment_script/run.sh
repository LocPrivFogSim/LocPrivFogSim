#!/bin/bash

rate=0.05
seed2=12
seed3=5
interval=0.1
threshold=0.0462
strategy="BelowThresholdRandomDevice"

time_begin=0
time_before=0
time_after=0
time_needed=0
#java -jar MobFogSim_privacy.jar 1 0.5 44 5 0.01 0

echo -e "\npossible paths, average path length, path hit, relative path hit, controlled area, accuracy, area hit duration" >> results/results.csv

time_begin=$(date +%s)

# Simulating offloading strategy BelowThresholdRandomDevice
# Threshold: 0.0462
strategy="BelowThresholdRandomDevice"
threshold=0.0462

for ((scenario=1;scenario<=2;scenario++))
do

	time_before=$(date +%s)

	for ((i=5;i<=100;i=$i+5))
	do
		rate=`echo "scale=2 ; $i/100" | bc`
		echo -e "\nScenario "$scenario",	rate of compromised devices: 0"$rate",	threshold: 0"$threshold"\n" >> results/results.csv
		echo -e "\noffloading strategy BelowThresholdRandomDevice" >> results/results.csv
		for ((j=0;j<100;j++))
		do
			let seed2=$RANDOM%50+1
			let seed3=$RANDOM%20
			'/mnt/c/Program Files (x86)/Common Files/Oracle/Java/javapath/java.exe' -jar MobFogSim_privacy.jar $scenario $rate $seed2 $seed3 $interval $threshold $strategy
		done
	done

	time_after=$(date +%s)
	let time_needed=$time_after-$time_before
	echo -e "time needed: "$time_needed" seconds\n\n" >> results/results.csv

done

# Simulating offloading strategy BelowThresholdRandomDevice
# Threshold: 0.0579
strategy="BelowThresholdRandomDevice"
threshold=0.0579

for ((scenario=1;scenario<=2;scenario++))
do

	time_before=$(date +%s)

	for ((i=5;i<=100;i=$i+5))
	do
		rate=`echo "scale=2 ; $i/100" | bc`
		echo -e "\nScenario "$scenario",	rate of compromised devices: 0"$rate",	threshold: 0"$threshold"\n" >> results/results.csv
		echo -e "\noffloading strategy BelowThresholdRandomDevice" >> results/results.csv
		for ((j=0;j<100;j++))
		do
			let seed2=$RANDOM%50+1
			let seed3=$RANDOM%20
			'/mnt/c/Program Files (x86)/Common Files/Oracle/Java/javapath/java.exe' -jar MobFogSim_privacy.jar $scenario $rate $seed2 $seed3 $interval $threshold $strategy
		done
	done

	time_after=$(date +%s)
	let time_needed=$time_after-$time_before
	echo -e "time needed: "$time_needed" seconds\n\n" >> results/results.csv

done


# Simulating offloading strategy BelowThresholdRandomDevice
# Threshold: 0.0696
strategy="BelowThresholdRandomDevice"
threshold=0.0696

for ((scenario=1;scenario<=2;scenario++))
do

	time_before=$(date +%s)

	for ((i=5;i<=100;i=$i+5))
	do
		rate=`echo "scale=2 ; $i/100" | bc`
		echo -e "\nScenario "$scenario",	rate of compromised devices: 0"$rate",	threshold: 0"$threshold"\n" >> results/results.csv
		echo -e "\noffloading strategy BelowThresholdRandomDevice" >> results/results.csv
		for ((j=0;j<100;j++))
		do
			let seed2=$RANDOM%50+1
			let seed3=$RANDOM%20
			'/mnt/c/Program Files (x86)/Common Files/Oracle/Java/javapath/java.exe' -jar MobFogSim_privacy.jar $scenario $rate $seed2 $seed3 $interval $threshold $strategy
		done
	done

	time_after=$(date +%s)
	let time_needed=$time_after-$time_before
	echo -e "time needed: "$time_needed" seconds\n\n" >> results/results.csv

done


####
#### Simulating other offloading strategy
####


# Simulating offloading strategy BelowThresholdLowestResponseTime
# Threshold: 0.0462
strategy="BelowThresholdLowestResponseTime"
threshold=0.0462

for ((scenario=1;scenario<=2;scenario++))
do

	time_before=$(date +%s)

	for ((i=5;i<=100;i=$i+5))
	do
		rate=`echo "scale=2 ; $i/100" | bc`
		echo -e "\nScenario "$scenario",	rate of compromised devices: 0"$rate",	threshold: 0"$threshold"\n" >> results/results.csv
		echo -e "\noffloading strategy BelowThresholdLowestResponseTime" >> results/results.csv
		for ((j=0;j<100;j++))
		do
			let seed2=$RANDOM%50+1
			let seed3=$RANDOM%20
			'/mnt/c/Program Files (x86)/Common Files/Oracle/Java/javapath/java.exe' -jar MobFogSim_privacy.jar $scenario $rate $seed2 $seed3 $interval $threshold $strategy
		done
	done

	time_after=$(date +%s)
	let time_needed=$time_after-$time_before
	echo -e "time needed: "$time_needed" seconds\n\n" >> results/results.csv

done

# Simulating offloading strategy BelowThresholdLowestResponseTime
# Threshold: 0.0579
strategy="BelowThresholdLowestResponseTime"
threshold=0.0579

for ((scenario=1;scenario<=2;scenario++))
do

	time_before=$(date +%s)

	for ((i=5;i<=100;i=$i+5))
	do
		rate=`echo "scale=2 ; $i/100" | bc`
		echo -e "\nScenario "$scenario",	rate of compromised devices: 0"$rate",	threshold: 0"$threshold"\n" >> results/results.csv
		echo -e "\noffloading strategy BelowThresholdLowestResponseTime" >> results/results.csv
		for ((j=0;j<100;j++))
		do
			let seed2=$RANDOM%50+1
			let seed3=$RANDOM%20
			'/mnt/c/Program Files (x86)/Common Files/Oracle/Java/javapath/java.exe' -jar MobFogSim_privacy.jar $scenario $rate $seed2 $seed3 $interval $threshold $strategy
		done
	done

	time_after=$(date +%s)
	let time_needed=$time_after-$time_before
	echo -e "time needed: "$time_needed" seconds\n\n" >> results/results.csv

done


# Simulating offloading strategy BelowThresholdLowestResponseTime
# Threshold: 0.0696
strategy="BelowThresholdLowestResponseTime"
threshold=0.0696

for ((scenario=1;scenario<=2;scenario++))
do

	time_before=$(date +%s)

	for ((i=5;i<=100;i=$i+5))
	do
		rate=`echo "scale=2 ; $i/100" | bc`
		echo -e "\nScenario "$scenario",	rate of compromised devices: 0"$rate",	threshold: 0"$threshold"\n" >> results/results.csv
		echo -e "\noffloading strategy BelowThresholdLowestResponseTime" >> results/results.csv
		for ((j=0;j<100;j++))
		do
			let seed2=$RANDOM%50+1
			let seed3=$RANDOM%20
			'/mnt/c/Program Files (x86)/Common Files/Oracle/Java/javapath/java.exe' -jar MobFogSim_privacy.jar $scenario $rate $seed2 $seed3 $interval $threshold $strategy
		done
	done

	time_after=$(date +%s)
	let time_needed=$time_after-$time_before
	echo -e "time needed: "$time_needed" seconds\n\n" >> results/results.csv

done

let time_needed=$time_after-$time_begin
echo -e "total time: "$time_needed" seconds\n" >> results/results.csv

echo -e "Fertig" >> results/results.csv
