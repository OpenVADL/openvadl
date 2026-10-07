#!/usr/bin/env bash
set -e

cd $(realpath $(dirname "$0"))

# miscompile
rm -r ../src/cubic

rm -r ../src/aha-mont64
#rm -r ../src/crc32
#rm -r ../src/crc32-dse-vadl
rm -r ../src/edn
rm -r ../src/huffbench
rm -r ../src/matmult-int
rm -r ../src/md5sum
rm -r ../src/minver
rm -r ../src/nbody
rm -r ../src/nettle-aes
rm -r ../src/nettle-sha256
rm -r ../src/nsichneu
rm -r ../src/picojpeg
rm -r ../src/primecount
rm -r ../src/qrduino
rm -r ../src/sglib-combined
rm -r ../src/slre
rm -r ../src/st
rm -r ../src/statemate
rm -r ../src/tarfind
rm -r ../src/ud
rm -r ../src/wikisort

bash ../build_spike-lcb-iss-O3_rv32.sh
bash ./run-benchmark.sh "rv32-lcb-iss-asm" bash ./benchmark_lcb_iss.sh
# cat results/rv32-lcb-iss-asm/1.json

