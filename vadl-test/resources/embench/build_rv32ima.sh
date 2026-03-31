#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

./build_all.py --arch native --chip default --board static --cc riscv32-unknown-linux-gnu-gcc --ld riscv32-unknown-linux-gnu-gcc --cflags="-march=rv32ima -mabi=ilp32" --clean "$@"
