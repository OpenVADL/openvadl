#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

./build_all.py --arch riscv64 --chip generic --board spike --clean "$@"
