#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

CFLAGS="-march=armv6 -marm -mfloat-abi=soft -g -mno-unaligned-access"
# benchmarks that use floating point types must be excluded
FLOATEXCL="cubic,nbody,minver,st,statemate,ud,wikisort"
./build_all.py --cc arm-none-eabi-gcc --arch aarch32 --chip generic --board virt-iss --clean --cflags "$CFLAGS" --exclude "$FLOATEXCL" "$@"
