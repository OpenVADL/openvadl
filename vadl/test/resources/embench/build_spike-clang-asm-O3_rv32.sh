#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

# Build picolibc and the compiler-rt builtins with the compiled LCB.
bash /lcb-runtime/build-runtime.sh \
    /opt/src/picolibc /src/llvm-final/compiler-rt/lib/builtins /opt/lcb-runtime/rv32im \
    /lcb-runtime/cross-rv32im.txt /lcb-runtime/machine-rv32im

./build_all.py -v --arch riscv32 --chip generic --board spike-lcb-asm-O3 --clean "$@"
