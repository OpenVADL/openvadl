#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

# exclude all but CRC
EXCLUDE="cubic,aha-mont64,edn,huffbench,matmult-int,md5sum,minver,nbody,nettle-aes,nettle-sha256,nsichneu,picojpeg,primecount,qrduino,sglib-combined,slre,st,statemate,tarfind,ud,wikisort"

python3 ./build_all.py -v --arch riscv32 --chip generic --board spike-lcb-iss-O3 --clean --exclude "$EXCLUDE" "$@"

