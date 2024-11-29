#!/usr/bin/env bash

if [ "$#" -lt 1 ]; then
	echo "Too few arguments"
	exit 1
elif [ "$#" -gt 1 ]; then
	echo "Too many arguments"
	exit 1
fi

./benchmark_speed.py --timeout 400 --json-output --absolute --target-module run_sim $(pwd)/__run_vadl.sh $1
