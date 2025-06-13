#!/usr/bin/env bash

set -x

# Compile from c source with the LCB Compiler
/src/llvm-final/build/bin/clang --target=${TARGET} -I/opt/riscv/riscv64-unknown-elf/include -S -O${OPT_LEVEL} -c /src/inputs/$INPUT -o /tmp/main.s
chmod 777 /tmp/main.s
cat /tmp/main.s

# Assemble with the LCB Assembler
/src/llvm-final/build/bin/llvm-mc -filetype=obj -arch=${TARGET} /tmp/main.s -o /tmp/main.o
/src/llvm-final/build/bin/llvm-mc -filetype=obj -arch=${TARGET} /helper/init.S -o /tmp/init.o

# Link with the LCB Linker
/src/llvm-final/build/bin/ld.lld -static -T/helper/link.ld /tmp/main.o /tmp/init.o -o /tmp/main
/work/elf_machine_updater update-machine --elf /tmp/main --value 243

# Execute with QEMU
qemu-system-${UPSTREAM_CLANG_TARGET} -d in_asm -nographic -machine spike -bios /tmp/main