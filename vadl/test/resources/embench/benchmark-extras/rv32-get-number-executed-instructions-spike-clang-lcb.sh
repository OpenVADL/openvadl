#!/usr/bin/env bash
cd $(realpath $(dirname "$0"))

SPIKE_TARGET='riscv32'

mkdir -p ../result

# First, we run the benchmarks for clang.
# This builds in the binaries in the `bd` folder.
# Then, we calculate the number of executed instructions for upstream.
sh run-rv32-benchmarks-spike-clang-upstream-O3.sh
sh ../qemu_get_number_executed_instructions.sh $SPIKE_TARGET
# Finally, we save the result for later.
mv ../bd/executed_instructions_absolute.csv ../result/executed_instructions_absolute_upstream.csv
mv ../bd/binary_size_absolute.csv ../result/binary_size_absolute_upstream.csv

echo "Upstream is done."

# Second, we run the benchmarks for the open vadl compiler.
# This builds in the binaries in the `bd` folder.
# Then, we calculate the number of executed instructions for downstream.
# Note that we rebuild upstream in the `run-benchmarks-spike-clang-lcb-O3.sh`. This is
# an unintentional side effect. We do not need that, but we want to execute the same
# downstream benchmarks but do not want to declare them twice.
sh rv32-run-benchmarks-spike-clang-lcb-O3.sh
sh ../qemu_get_number_executed_instructions.sh $SPIKE_TARGET
# Finally, we save the result for later.
mv ../bd/executed_instructions_absolute.csv ../result/executed_instructions_absolute_lcb.csv
mv ../bd/binary_size_absolute.csv ../result/binary_size_absolute_lcb.csv

echo "LCB is done."

# This calculates the relative performance between upstream and lcb.
# It will also save the result into a csv and return an arithmetic mean for the relative performance.
MEAN=`python3 ../executed_instructions_relative_performance.py ../result/executed_instructions_absolute_upstream.csv ../result/executed_instructions_absolute_lcb.csv`

echo "ABSOLUTE UPSTREAM"
cat ../result/executed_instructions_absolute_upstream.csv
echo "ABSOLUTE LCB"
cat ../result/executed_instructions_absolute_lcb.csv
echo "RELATIVE"
cat ../result/executed_instructions_relative.csv
echo $MEAN

# Compare the .text section sizes of the binaries that were executed above.
python3 ../executed_instructions_relative_performance.py \
  ../result/binary_size_absolute_upstream.csv ../result/binary_size_absolute_lcb.csv \
  --output ../result/binary_size_relative.csv --metric '.text size (bytes)'
echo ".text SIZE (benchmark, upstream bytes, LCB bytes, upstream / LCB)"
cat ../result/binary_size_relative.csv
