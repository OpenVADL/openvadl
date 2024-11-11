#!/usr/bin/env bash

set -x

/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -c /tmp/main.s -o /tmp/main.o
/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -c /helper/init.s -o /helper/init.o
/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -c /helper/trap.s -o /helper/trap.o
/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -c /helper/vars.spike.s -o /helper/vars.spike.o
/src/llvm-final/build/bin/clang --target=${UPSTREAM_CLANG_TARGET} -c /helper/common.s -o /helper/common.o
/opt/riscv-cross/bin/riscv64-unknown-linux-gnu-gcc -static -nostartfiles -T/helper/link_lcbw.ld /tmp/main.o /helper/init.o /helper/trap.o /helper/vars.spike.o /helper/common.o -o /tmp/main

echo "Running spike..."
/opt/spike/bin/spike --isa=rv64im /tmp/main
