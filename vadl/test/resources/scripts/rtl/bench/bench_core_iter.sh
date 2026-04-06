#!/bin/bash

TOP_MODULE="$1"
lo="$2"
hi="$3"
temp=`mktemp`

if [ -z "$TOP_MODULE" -o -z "$lo" -o -z "$hi" ]; then
  echo "Usage: $0 <top_module> <clock_period_min> <clock_period_max>"
  exit 1
fi

best=hi

while awk "BEGIN { exit !(($hi - $lo) > 0.1) }"; do
  mid=`awk "BEGIN { print (($hi + $lo) * 0.5) }"`
  echo "$TOP_MODULE $mid"
  if /scripts/bench/bench_core.sh "$TOP_MODULE" $mid | tee $temp | tail -n10 | grep "(MET)" > /dev/null; then
    echo "timing met"
    best=$mid
    hi=$mid
  else
    echo "timing violated"
    lo=$mid
  fi
done

echo "==========================================="
echo "  $TOP_MODULE : best clock $best ns"
echo "==========================================="

grep "INFO GPL-10" $temp
grep "data arrival time" $temp

rm $temp
