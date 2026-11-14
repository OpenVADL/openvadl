#!/usr/bin/env bash
set -eu

cd "$(dirname "$0")"

case "${1-}" in
  riscv32|riscv64) ;;
  *) echo "Usage: $0 riscv32|riscv64" >&2; exit 2 ;;
esac

# Each invocation measures the current build, before another compiler replaces it.
: > bd/executed_instructions_absolute.csv
: > bd/binary_size_absolute.csv

for dir in bd/src/*; do
  # The name of the binary is the same as the directory.
  # That's why $dir/$dir
  base=$(basename "$dir")
  binary="$dir/$base"
  echo "Executing $dir/$base"
  # Measure only .text, excluding debug information and other ELF sections.
  SECTIONS=$(riscv64-unknown-elf-size -A -d "$binary")
  SIZE=$(printf '%s\n' "$SECTIONS" | awk '$1 == ".text" { print $2; found = 1 } END { if (!found) exit 1 }')
  # The triggers emit harmless slti instructions writing to the zero register.
  # Sum QEMU's deltas between markers, excluding the stop marker's instruction.
  log="$dir/instruction_count.log"
  if ! /opt/qemu/bin/qemu-system-"$1" \
    -plugin '/opt/qemu/plugins/plugins/libinsn.so,match=slti ,trace=on' \
    -nographic -machine spike -smp 1 -bios "$binary" -d plugin -D "$log"; then
    echo "Benchmark $base failed" >&2
    exit 1
  fi
  if ! EXEC=$(awk -F 'Δ[+]| since last match' '
    /since last match/ {
      if (active) total += $2
      if (/slti +zero,zero,1[^0-9]/) { starts++; active = 1 }
      if (/slti +zero,zero,2[^0-9]/) { stops++; active = 0 }
    }
    END {
      if (starts != 1 || stops != 1 || active || total <= 1) exit 1
      printf "%.0f\n", total - 1
    }
  ' "$log"); then
    echo "Missing or invalid benchmark instruction count for $base (see $log)" >&2
    exit 1
  fi
  echo "$base,$EXEC" >> "bd/executed_instructions_absolute.csv"
  echo "$base,$SIZE" >> "bd/binary_size_absolute.csv"
  echo "Iteration $dir: Exec instructions: $EXEC. .text size: $SIZE bytes."
done
