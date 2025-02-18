#!/usr/bin/env bash
OUTPUT=$((time -p $1 -s ./signature -t 1000000000 $2 2> /dev/null ) 2>&1)
echo $OUTPUT | sed -nE 's/.*machine cycles: ([0-9]+).*/TIME=\1.0/p'

NUM=$(cat signature)
RET=$((16#$NUM))

exit $RET
