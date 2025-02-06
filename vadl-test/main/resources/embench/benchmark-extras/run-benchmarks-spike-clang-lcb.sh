#!/usr/bin/env bash
set -e

cd $(realpath $(dirname "$0"))

# Spike
../build_spike-clang.sh
#./run-benchmark.sh "rv32-spike" ./benchmark_spike.sh

# miscompile
rm -r ../src/cubic
# long jump problem
rm -r ../src/statemate

# aha-mont64
# edn
# sglib-combined
# crc32

#rm -r ../src/huffbench
#rm -r ../src/matmult-int
#rm -r ../src/md5sum

#rm -r ../src/minver
#rm -r ../src/nbody

#rm -r ../src/nettle-aes

#rm -r ../src/nettle-sha256
#rm -r ../src/nsichneu
#rm -r ../src/picojpeg
#rm -r ../src/primecount
#rm -r ../src/qrduino
#rm -r ../src/slre
#rm -r ../src/st
#rm -r ../src/tarfind
#rm -r ../src/ud
#rm -r ../src/wikisort

../build_spike-lcb.sh
#./run-benchmark.sh "rv32-spike" ./benchmark_spike.sh
#cat /src/embench/benchmark-extras/results/rv32-spike/1.json