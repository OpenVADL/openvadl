#!/usr/bin/env bash
set -e

cd $(realpath $(dirname "$0"))

# Spike
../build_spike-clang-O0_rv32.sh
./run-benchmark.sh "rv32-spike" ./benchmark_spike.sh

# miscompile
rm -r ../src/cubic
# long jump problem
rm -r ../src/statemate

# In file included from /src/embench/src/wikisort/libwikisort.c:30:
# In file included from /opt/riscv-cross/riscv64-unknown-elf/include/limits.h:132:
# In file included from /src/llvm-final/build/lib/clang/17/include/limits.h:21:
# /usr/include/limits.h:145:5: error: function-like macro '__GLIBC_USE' is not defined
#  145 | #if __GLIBC_USE (IEC_60559_BFP_EXT_C2X)
rm -r ../src/wikisort

# aha-mont64
# edn
# sglib-combined
# crc32

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
#rm -r ../src/slre
#rm -r ../src/st
#rm -r ../src/tarfind
#rm -r ../src/ud
#rm -r ../src/wikisort

../build_spike-lcb-O0_rv32.sh
./run-benchmark.sh "rv32-spike" ./benchmark_spike.sh
#cat /src/embench/benchmark-extras/results/rv32-spike/1.json