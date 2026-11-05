#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

# Build picolibc and the compiler-rt builtins with the compiled LCB.
bash /lcb-runtime/build-runtime.sh \
    /opt/src/picolibc /src/llvm-final/compiler-rt/lib/builtins /opt/lcb-runtime/rv32im \
    /lcb-runtime/cross-rv32im.txt /lcb-runtime/machine-rv32im

# exclude all but CRC
EXCLUDE="cubic,aha-mont64,edn,huffbench,matmult-int,md5sum,minver,nbody,nettle-aes,nettle-sha256,nsichneu,picojpeg,primecount,qrduino,sglib-combined,slre,st,statemate,tarfind,ud,wikisort"

python3 ./build_all.py -v --arch riscv32 --chip generic --board spike-lcb-iss-O3 --clean --exclude "$EXCLUDE" "$@"

