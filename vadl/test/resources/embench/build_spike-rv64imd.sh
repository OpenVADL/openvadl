#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

arch="rv64imd"
abi="lp64d"

cflags="-march=$arch -mabi=$abi -std=gnu17"
ldflags="-march=$arch -mabi=$abi"
# non-float: aha-mont64,huffbench,md5sum,nettle-sha256,picojpeg,qrduino,slre,statemate,crc32,edn,matmult-int,nettle-aes,nsichneu,primecount,sglib-combined,tarfind,wikisort
# float:
#   cubic: uses long double, which requires either Q (quad-float) extension or soft-float. Soft-float requires csr and fcsr
#   nbody: works with D extension
#   minver: works with D extension
#   st: requires csr instructions to work with fcsr (because of quiet comparisons, which are emitted by the compiler using csr instructions to save/restore fflags)
#   ud: works with D extension

# need fcsr to work with csr instructions
EXCL="cubic,st"

./build_all.py --verbose --arch riscv64 --chip generic --board spike --cflags="$cflags" --ldflags="$ldflags" --clean --exclude "$EXCL" "$@"
