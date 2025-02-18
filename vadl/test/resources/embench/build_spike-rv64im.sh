#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

arch="rv64im"
abi="lp64"

cflags="-march=$arch -mabi=$abi"
ldflags="-march=$arch -mabi=$abi"
./build_all.py --arch riscv64 --chip generic --board spike --cflags="$cflags" --ldflags="$ldflags" --clean  "$@"
