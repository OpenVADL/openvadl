#!/usr/bin/env bash
set -e

cd $(realpath $(dirname "$0"))

bash ../build_spike-lcb-iss-O3_rv32.sh
bash ./run-benchmark.sh "rv32-lcb-iss-asm" bash ./benchmark_spike.sh
# cat results/rv32-lcb-iss-asm/1.json

