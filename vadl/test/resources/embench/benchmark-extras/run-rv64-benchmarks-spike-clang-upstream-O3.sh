#!/usr/bin/env bash
set -e

cd $(realpath $(dirname "$0"))

# Spike
../build_spike-clang-O3_rv64.sh
./run-benchmark.sh "rv64-spike" ./benchmark_spike_rv64gc.sh