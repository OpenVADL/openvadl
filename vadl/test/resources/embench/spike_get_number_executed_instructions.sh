#!/usr/bin/env bash

cd $(realpath $(dirname "$0"))

for dir in bd/src/*; do
  # The name of the binary is the same as the directory.
  # That's why $dir/$dir
  base=`basename $dir`
  EXEC=`/opt/spike/bin/spike --isa=$1 -l $dir/$base 2>&1 | wc -l | tail -1`
  echo "$base,$EXEC" >> "bd/executed_instructions_absolute.csv"
  echo "Iteration $dir: Spike execution completed. Exec instructions: $EXEC."
done