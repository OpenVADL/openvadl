#!/usr/bin/env bash
set -e

cd $(realpath $(dirname "$0"))

# Spike
../build_spike-clang.sh
./run-benchmark.sh "rv32-spike" ./benchmark_spike.sh

../build_spike-lcb.sh
./run-benchmark.sh "rv32-spike" ./benchmark_spike.sh

git checkout ../src