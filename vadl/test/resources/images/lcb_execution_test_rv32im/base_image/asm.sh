#!/usr/bin/env bash

set -x

/src/llvm-final/build/bin/llvm-mc -filetype=obj -arch=${TARGET} /src/inputs/$INPUT -o /tmp/main.o
/src/llvm-final/build/bin/llvm-mc -filetype=obj -arch=${TARGET} /helper/init.S -o /tmp/init.o

/src/llvm-final/build/bin/ld.lld -static -T/helper/link.ld /tmp/main.o /tmp/init.o -o /tmp/main
/work/elf_machine_updater update-machine --elf /tmp/main --value 243

qemu-system-${UPSTREAM_CLANG_TARGET} -d in_asm -nographic -machine spike -bios /tmp/main