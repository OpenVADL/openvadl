#!/usr/bin/env bash
OUTPUT=$((time -p spike --isa=rv32ima $1 ) 2>&1)
RET=$?
echo $OUTPUT | sed -nE 's/real[[:space:]]*([0-9]+)\.([0-9]+).*/TIME=\1.\2/p'

exit $RET
