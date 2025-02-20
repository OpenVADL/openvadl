#!/usr/bin/env bash
set -e

cd $(realpath $(dirname "$0"))

SPIKE_TARGET='rv32im'

mkdir ../result

# First, we run the benchmarks for clang.
# This builds in the binaries in the `bd` folder.
# Then, we calculate the number of executed instructions for upstream.
sh run-benchmarks-spike-clang-upstream-O3.sh
sh ../get_number_executed_instructions.sh $SPIKE_TARGET
# Finally, we save the result for later.
mv ../bd/executed_instructions_absolute.csv ../result/executed_instructions_absolute_upstream.csv

echo "Upstream is done."

# Second, we run the benchmarks for the open vadl compiler.
# This builds in the binaries in the `bd` folder.
# Then, we calculate the number of executed instructions for downstream.
# Note that we rebuild upstream in the `run-benchmarks-spike-clang-lcb-O3.sh`. This is
# an unintentional side effect. We do not need that, but we want to execute the same
# downstream benchmarks but do not want to declare them twice.
sh run-benchmarks-spike-clang-lcb-O3.sh
sh ../get_number_executed_instructions.sh $SPIKE_TARGET
# Finally, we save the result for later.
mv ../bd/executed_instructions_absolute.csv ../result/executed_instructions_absolute_lcb.csv

echo "LCB is done."

# This calculates the relative performance between upstream and lcb.
# It will also save the result into a csv and return a geometric mean for the relative performance.
MEAN=`python3 ../executed_instructions_relative_performance.py ../result/executed_instructions_absolute_upstream.csv ../result/executed_instructions_absolute_lcb.csv`

echo "ABSOLUTE UPSTREAM"
cat ../result/executed_instructions_absolute_upstream.csv
echo "ABSOLUTE LCB"
cat ../result/executed_instructions_absolute_lcb.csv
echo "RELATIVE"
cat ../result/executed_instructions_relative.csv
echo $MEAN