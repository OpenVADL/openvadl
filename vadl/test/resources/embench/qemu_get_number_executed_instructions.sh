#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

for dir in bd/src/*; do
  # The name of the binary is the same as the directory.
  # That's why $dir/$dir
  base=`basename $dir`
  echo "Executing $dir/$base"
  EXEC=`QEMU_PLUGIN_LOG=1 DEBUG=1 /opt/qemu/bin/qemu-system-$1 -plugin /opt/qemu/plugins/plugins/libinsn.so -nographic -machine spike -bios $dir/$base -d plugin -D log.txt && tail -n 1 log.txt | grep -oE '[0-9]+$'`
  echo "$base,$EXEC" >> "bd/executed_instructions_absolute.csv"
  echo "Iteration $dir: Exec instructions: $EXEC."
done