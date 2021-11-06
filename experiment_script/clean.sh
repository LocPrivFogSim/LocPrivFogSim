#!/bin/sh

echo "Cleanup simulation artifacts"

rm -v *.txt
rm -v *.csv
rm -v averages/*
rm -v privacy/*
rm -v results/*
rm -v experiment_script/*.txt