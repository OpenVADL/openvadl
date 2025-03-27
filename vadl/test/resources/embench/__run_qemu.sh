#!/usr/bin/env bash

OUTPUT=$((time -p "$@" ) 2>&1)

echo $OUTPUT | sed -nE 's/real[[:space:]]*([0-9]+)\.([0-9]+).*/TIME=\1.\2/p'

exit $RET
