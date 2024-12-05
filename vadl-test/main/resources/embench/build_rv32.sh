#!/usr/bin/env bash

./build_all.py --arch native --chip default --board static --cc riscv32-unknown-linux-gnu-gcc --ld riscv32-unknown-linux-gnu-gcc --clean "$@"
