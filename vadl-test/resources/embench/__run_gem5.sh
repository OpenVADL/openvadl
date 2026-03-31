#!/usr/bin/env bash
$1 $2 --binary $3
RET=$?
sed -nE 's/hostSeconds[[:space:]]*([0-9]+)\.([0-9]+).*/TIME=\1.\2/p' m5out/stats.txt

# Extract number of cycles
sed -nE 's/.*numCycles[[:space:]]*([0-9]+).*/\1/p' m5out/stats.txt > cycles.txt

exit $RET
