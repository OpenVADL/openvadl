#!/usr/bin/env bash

set -e

cd $(realpath $(dirname "$0"))

VADL_DTC=$(realpath "../bd-vadl/vadl-dtc-aarch64")  # $(realpath ../../vadl/sys/aarch64/obj/newcpu2/CPU/iss/dtc/obj/rel/CPU)
GEM5_BIN=gem5.fast  # $(realpath ../../gem5/build/ARM/gem5.opt)
QEMU_PREFIX=/opt/qemu/bin
QEMU_NOJIT_PREFIX=/opt/qemu-nojit/bin # compiled with --enable-tcg-interpreter

# These would be FSE instead of UME
# ../build_aarch64.sh semihosting
# ./run-benchmark.sh "aarch64-qemu"                  ./benchmark_qemu.sh       "$QEMU_PREFIX/qemu-system-aarch64" -M virt-8.2 -cpu cortex-a57 -m 4G -net none -nographic -semihosting -accel tcg,one-insn-per-tb=off -kernel
# ./run-benchmark.sh "aarch64-qemu-singlestep"       ./benchmark_qemu.sh       "$QEMU_PREFIX/qemu-system-aarch64" -M virt-8.2 -cpu cortex-a57 -m 4G -net none -nographic -semihosting -accel tcg,one-insn-per-tb=on  -kernel
# ./run-benchmark.sh "aarch64-qemu-nojit"            ./benchmark_qemu.sh "$QEMU_NOJIT_PREFIX/qemu-system-aarch64" -M virt-8.2 -cpu cortex-a57 -m 4G -net none -nographic -semihosting -accel tcg,one-insn-per-tb=off -kernel

../build_aarch64.sh ume # --cpu-mhz 100
./run-benchmark.sh "aarch64-qemu"                  ./benchmark_qemu.sh       "$QEMU_PREFIX/qemu-aarch64"
./run-benchmark.sh "aarch64-qemu-singlestep"       ./benchmark_qemu.sh       "$QEMU_PREFIX/qemu-aarch64" -one-insn-per-tb
./run-benchmark.sh "aarch64-qemu-nojit"            ./benchmark_qemu.sh "$QEMU_NOJIT_PREFIX/qemu-aarch64" 
# ./run-benchmark.sh "aarch64-dtc"                   ./benchmark_qemu.sh $VADL_DTC

# ./run-benchmark.sh "aarch64-gem5"                  ./benchmark_gem5.sh "$GEM5_BIN" "$(realpath ../gem5-scripts/atomic-armv8.py)"

python3 data-relative.py results-aarch64-dtc results/aarch64-qemu/aarch64-qemu.csv \
        results/aarch64-qemu-singlestep/aarch64-qemu-singlestep.csv \
        results/aarch64-qemu-nojit/aarch64-qemu-nojit.csv

#       results/aarch64-dtc/aarch64-dtc.csv \
