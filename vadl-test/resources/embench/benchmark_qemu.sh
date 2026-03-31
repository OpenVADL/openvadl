#!/usr/bin/env bash

if [ "$#" -lt 1 ]; then
	echo "You need to pass the base qemu invocation, like"
	echo "	$0 qemu-riscv32"
	echo "	or"
	echo "	$0 qemu-system-aarch64 -M virt-8.2 -cpu cortex-a57 -m 16G -net none -nographic -semihosting -kernel"
	exit 1
fi

# Useful tracing flags for qemu: `-d in_asm,cpu,exec,int`

./benchmark_speed.py --timeout 60 --json-output --absolute --target-module run_sim $(pwd)/__run_qemu.sh "$@"
