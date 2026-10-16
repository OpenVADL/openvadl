#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

EXCLUDE="cubic,huffbench,minver,nbody,nettle-aes,nettle-sha256,slre,st,wikisort"

./build_all.py -v --arch riscv32 --chip generic --board spike-lcb-asm-O3 --clean --exclude "$EXCLUDE" "$@"
