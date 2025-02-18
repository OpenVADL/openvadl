#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

./build_all.py --arch riscv32 --chip generic --board spike --cc riscv32-unknown-linux-gnu-gcc --ld riscv32-unknown-linux-gnu-gcc --clean "$@"
