#!/usr/bin/env bash

set -x

/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -c -O0 /tmp/main.s -o /tmp/main.o
/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -c -O0 /helper/init.s -o /helper/init.o
/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -c -O0 /helper/trap.s -o /helper/trap.o
/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -c -O0 /helper/vars.spike.s -o /helper/vars.spike.o
# We need to compile -fPIC here because otherwise we get a relocation error that it got truncated.
# Not sure, why though? Maybe, the linker script but all .text are next to each other.
# Maybe, the tohost section is too far away.
/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -fPIC -c -O0 /helper/common.c -o /helper/common.o
/opt/riscv-cross/bin/riscv64-unknown-linux-gnu-gcc -static -nostartfiles -T/helper/link_lcbw.ld /tmp/main.o /helper/init.o /helper/trap.o /helper/vars.spike.o /helper/common.o -o /tmp/main

echo "Running spike..."
/opt/spike/bin/spike --isa=${SPIKE_TARGET} /tmp/main
