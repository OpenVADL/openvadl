#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

CFLAGS="-march=armv8-a+nofp+nosimd "
EXCL="cubic,nbody,st,minver,statemate,wikisort,ud"
TMPEXCL=",huffbench,md5sum,qrduino,sglib-combined,tarfind"
FAILING=",edn,matmult-int,nettle-sha256,picojpeg,slre"
./build_all.py --cc aarch64-none-elf-gcc --arch aarch64 --chip generic --board virt-iss --clean --cflags "$CFLAGS" --exclude "$EXCL$TMPEXCL$FAILING"  "$@"
