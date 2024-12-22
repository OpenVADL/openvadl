#!/usr/bin/env bash

set -e
set -x

/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -c -march=${SPIKE_TARGET} -O0 /tmp/main.s -o /tmp/main.o
/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -c -march=${SPIKE_TARGET} -O0 /helper/init.s -o /helper/init.o
/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -c -march=${SPIKE_TARGET} -O0 /helper/trap.s -o /helper/trap.o
/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -c -march=${SPIKE_TARGET} -O0 /helper/vars.spike.s -o /helper/vars.spike.o
/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -c -march=${SPIKE_TARGET} -O0 /helper/common.c -o /helper/common.o
/opt/riscv-cross/bin/riscv32-unknown-linux-gnu-gcc -static -nostartfiles -T/helper/link_lcbw.ld /tmp/main.o /helper/init.o /helper/trap.o /helper/vars.spike.o /helper/common.o -o /tmp/main

echo "Running spike..."
timeout --preserve-status 5 /opt/spike/bin/spike --isa=${SPIKE_TARGET} /tmp/main