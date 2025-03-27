#!/usr/bin/env bash

# This runs the benchmark 5 times and stores the intermediate and final csv results in the specified subfolder
# Usage
# ./run-benchmark.sh "name" "./benchmark_*.sh" <args to pass to the specified script>

set -e

cwd=$(realpath $(dirname "$0"))

name=$1
runner=$2
shift
shift

rm -rf "results/$name"
mkdir -p "results/$name"

cd ..

for i in $(seq 1 10); do
    $runner "$@" > "benchmark-extras/results/$name/$i.json"
done

cd "benchmark-extras"

python3 data-eval.py "results/$name"
mv aggregated.csv "results/$name/$name.csv"
if [ -f cycles.csv ]; then
	mv cycles.csv "results/$name/cycles.csv"
fi
