#!/usr/bin/env bash
set -e

cd $(realpath $(dirname "$0"))

# FIXME Only those benchmarks are
# known to compile and run correctly
# with LCB.
# rm -rf ../src/aha-mont64
# rm -rf ../src/crc32
rm -rf ../src/cubic             # miscompile
# rm -rf ../src/edn
# rm -rf ../src/huffbench
# rm -rf ../src/matmult-int
# rm -rf ../src/md5sum
# rm -rf ../src/minver
# rm -rf ../src/nbody
# rm -rf ../src/nettle-aes
rm -rf ../src/nettle-sha256     # long jump problem
rm -rf ../src/nsichneu          # long jump problem
# rm -rf ../src/picojpeg
# rm -rf ../src/primecount
# rm -rf ../src/qrduino
# rm -rf ../src/sglib-combined
# rm -rf ../src/slre
# rm -rf ../src/st
# rm -rf ../src/statemate
# rm -rf ../src/tarfind
# rm -rf ../src/ud
rm -rf ../src/wikisort          # long jump problem

# FIXME Refactor benchmark_vadl_cycles.sh
# use benchmark_vadl.sh with an additional argument
# to select cycles vs runtime
../build_vadl-clang_rv32.sh
./run-benchmark.sh rv32-clang-cas-p3 ./benchmark_vadl_cycles.sh $(realpath ../bd-vadl/vadl-cas-rv32i_m_p3)

../build_vadl-lcb_rv32.sh
./run-benchmark.sh rv32-lcb-cas-p3 ./benchmark_vadl_cycles.sh $(realpath ../bd-vadl/vadl-cas-rv32i_m_p3)

git checkout ../src
