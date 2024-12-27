#!/usr/bin/env bash
set -e

cd $(realpath $(dirname "$0"))

# Spike
../build_spike-clang.sh
./run-benchmark.sh "rv32-spike" ./benchmark_spike.sh

# miscompile
rm -r ../src/cubic
# long jump problem
rm -r ../src/nettle-sha256
rm -r ../src/nsichneu
rm -r ../src/wikisort
rm -r ../src/statemate

../build_spike-lcb.sh
./run-benchmark.sh "rv32-spike" ./benchmark_spike.sh
cat /src/embench/benchmark-extras/results/rv32-spike/1.json