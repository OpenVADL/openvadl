#!/usr/bin/env bash
OUTPUT=$((time -p $1 -s ./signature -t 1000000000 $2 2> /dev/null ) 2>&1)
echo $OUTPUT | sed -nE 's/.*real[[:space:]]*([0-9]+)\.([0-9]+).*/TIME=\1.\2/p'
echo $OUTPUT | sed -nE 's/.*machine cycles: ([0-9]+).*/\1/p' > cycles.txt
echo $OUTPUT > output.txt

NUM=$(cat signature)
RET=$((16#$NUM))

exit $RET
