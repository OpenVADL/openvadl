#!/usr/bin/env bash

if [ "$#" -lt 2 ]; then
	echo "Too few arguments"
	exit 1
elif [ "$#" -gt 2 ]; then
	echo "Too many arguments"
	exit 1
fi

./benchmark_speed.py --json-output --absolute --target-module run_sim $(pwd)/__run_gem5.sh $1 $2
