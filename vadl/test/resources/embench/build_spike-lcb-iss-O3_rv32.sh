#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

python3 ./build_all.py -v --arch riscv32 --chip generic --board spike-lcb-iss-O3 --clean "$@"

