#!/usr/bin/env bash

set -e

cwd=$(realpath $(dirname "$0"))

flags=""

if [[ "$1" == "vadl" ]]; then
	flags="-D STOP_VADL_BRK"
elif [[ "$1" == "ume" ]]; then
	flags="-D STOP_LINUX_UME"
elif [[ "$1" == "semihosting" ]]; then
	flags="-D STOP_SEMIHOISTING"
else
  echo "Usage: $0 <vadl|ume|semihosting> ..."
  echo "Possible modes:"
  echo "	vadl: 		Set signature and stop using \`brk #0\`"
  echo "	ume: 		Stop using the linux SYS_exit syscall (e.g. for user mode emulation with gem5)"
  echo "	semihosting: 	Stop using the ARM semihosting syscall-like-interface (e.g. for qemu-system)"
  exit
fi
shift

"$cwd/build_all.py" --arch arm --chip cortex-m4 --board vadl --cflags "$flags" --clean "$@"
