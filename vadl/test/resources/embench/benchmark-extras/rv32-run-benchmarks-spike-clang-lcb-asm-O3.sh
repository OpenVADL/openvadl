#!/usr/bin/env bash
set -e

cd $(realpath $(dirname "$0"))

# Spike
#../build_spike-clang-O3_rv32.sh
#./run-benchmark.sh "rv32-spike" ./benchmark_spike.sh

../build_spike-clang-asm-O3_rv32.sh
./run-benchmark.sh "rv32-spike-asm" ./benchmark_spike.sh
#cat /src/embench/benchmark-extras/results/rv32-spike-asm/1.json