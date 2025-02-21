#!/usr/bin/env bash
set -e

cd $(realpath $(dirname "$0"))

# Spike
../build_spike-clang-O3.sh
./run-benchmark.sh "rv32-spike" ./benchmark_spike.sh