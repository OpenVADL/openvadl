#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

./build_all.py -v --arch riscv64 --chip generic --board spike-clang-O3 --clean "$@"
