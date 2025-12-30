#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

CFLAGS="-march=armv8-a -g -mstrict-align"
# benchmarks that use floating point types must be excluded
FLOATEXCL="cubic,nbody,minver,st,statemate,ud,wikisort"
./build_all.py --cc aarch64-none-elf-gcc --arch aarch64 --chip generic --board virt-iss --clean --cflags "$CFLAGS" --exclude "$FLOATEXCL" "$@"
