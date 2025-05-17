#!/usr/bin/env bash
set -e

cd $(realpath $(dirname "$0"))

# Spike
rm -r ../src/cubic

../build_spike-clang-O0_rv64.sh
./run-benchmark.sh "rv64-spike" ./benchmark_spike_rv64gc.sh

#rm -r ../src/aha-mont64
#rm -r ../src/crc32
#rm -r ../src/edn
#rm -r ../src/huffbench
#rm -r ../src/md5sum
#rm -r ../src/matmult-int
#rm -r ../src/minver
#rm -r ../src/nbody
#rm -r ../src/nettle-aes
#rm -r ../src/nettle-sha256
#rm -r ../src/nsichneu
#rm -r ../src/picojpeg
#rm -r ../src/primecount
#rm -r ../src/qrduino
#rm -r ../src/sglib-combined
rm -r ../src/slre
#rm -r ../src/st
#rm -r ../src/statemate
#rm -r ../src/tarfind
#rm -r ../src/ud
rm -r ../src/wikisort

../build_spike-lcb-O0_rv64.sh
./run-benchmark.sh "rv64-spike" ./benchmark_spike_rv64gc.sh