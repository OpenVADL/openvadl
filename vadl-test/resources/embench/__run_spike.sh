#!/usr/bin/env bash
OUTPUT=$( { /usr/bin/time -p qemu-system-riscv32 -L /opt/riscv/riscv64-unknown-elf -nographic -machine spike -bios "$1"; } 2>&1 )
RET=$?
echo $OUTPUT | sed -nE 's/real[[:space:]]*([0-9]+)\.([0-9]+).*/TIME=\1.\2/p'

exit $RET
