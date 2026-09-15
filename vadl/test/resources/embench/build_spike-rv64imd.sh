#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

arch="rv64imd"
abi="lp64d"

cflags="-march=$arch -mabi=$abi"
ldflags="-march=$arch -mabi=$abi"
# non-float: aha-mont64,huffbench,md5sum,nettle-sha256,picojpeg,qrduino,slre,statemate,crc32,edn,matmult-int,nettle-aes,nsichneu,primecount,sglib-combined,tarfind,wikisort
# float: cubic,nbody,minver,st,ud
./build_all.py --arch riscv64 --chip generic --board spike --cflags="$cflags" --ldflags="$ldflags" --clean "$@"
