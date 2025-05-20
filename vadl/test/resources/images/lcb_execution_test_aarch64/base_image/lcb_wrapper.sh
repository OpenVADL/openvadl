#!/usr/bin/env bash

set -x

/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -march=${TARGET} -fPIC -c -O0 /tmp/main.s -o /tmp/main.o
/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -march=${TARGET} -c -O0 /helper/init.S -o /helper/init.o
/opt/riscv-cross/bin/riscv64-unknown-elf-gcc -march=${TARGET} -mabi=${ABI} -static -nostartfiles -T/helper/link.ld /tmp/main.o /helper/init.o -o /tmp/main

echo "Running spike..."
qemu-system-${UPSTREAM_CLANG_TARGET} -L /opt/riscv-cross/riscv64-unknown-elf -nographic -machine spike -bios /tmp/main