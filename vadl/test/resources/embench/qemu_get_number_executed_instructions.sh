#!/usr/bin/env bash
set -e

cd "$(dirname "$0")"

# Each invocation measures the current build, before another compiler replaces it.
: > bd/executed_instructions_absolute.csv
: > bd/binary_size_absolute.csv

for dir in bd/src/*; do
  # The name of the binary is the same as the directory.
  # That's why $dir/$dir
  base=$(basename "$dir")
  binary="$dir/$base"
  echo "Executing $dir/$base"
  SIZE=$(wc -c < "$binary" | tr -d '[:space:]')
  EXEC=$(QEMU_PLUGIN_LOG=1 DEBUG=1 /opt/qemu/bin/qemu-system-$1 -plugin /opt/qemu/plugins/plugins/libinsn.so -nographic -machine spike -bios "$binary" -d plugin -D log.txt && tail -n 1 log.txt | grep -oE '[0-9]+$')
  echo "$base,$EXEC" >> "bd/executed_instructions_absolute.csv"
  echo "$base,$SIZE" >> "bd/binary_size_absolute.csv"
  echo "Iteration $dir: Exec instructions: $EXEC. Binary size: $SIZE bytes."
done
