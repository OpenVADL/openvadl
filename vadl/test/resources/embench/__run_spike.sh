#!/usr/bin/env bash
OUTPUT=$( { /usr/bin/time -p "${QEMU_SYSTEM:-qemu-system-riscv32}" -L /opt/riscv/riscv64-unknown-elf -nographic -machine "${QEMU_MACHINE:-spike}" -bios "$1"; } 2>&1 )
RET=$?
printf '%s\n' "$OUTPUT" | sed -nE 's/^real[[:space:]]*([0-9]+)\.([0-9]+).*/TIME=\1.\2/p'
printf '%s\n' "$OUTPUT" >&2

exit $RET
