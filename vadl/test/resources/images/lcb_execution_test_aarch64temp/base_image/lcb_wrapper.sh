#!/usr/bin/env bash

set -x

/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -c -O0 /tmp/main.s -o /tmp/main.o
#/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -c -O0 /helper/init.S -o /helper/init.o
/opt/aarch64/bin/aarch64-none-linux-gnu-gcc -mabi=${ABI} -static /tmp/main.o -o /tmp/main

echo "Running spike..."
qemu-${UPSTREAM_CLANG_TARGET} -L /opt/aarch64/aarch64-none-linux-gnu/include /tmp/main