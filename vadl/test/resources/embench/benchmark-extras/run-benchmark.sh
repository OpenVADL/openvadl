#!/usr/bin/env bash

# Run 10 trials and write raw timings and per-benchmark minima.
# Usage
# ./run-benchmark.sh "name" "./benchmark_*.sh" <args to pass to the specified script>

set -e

cd "$(dirname "$(realpath "$0")")"

name=$1
runner=$2
shift
shift

rm -rf "results/$name"
mkdir -p "results/$name"
cp ../bd/build-metadata.json "results/$name/build-metadata.json"

cd ..

for i in $(seq 1 10); do
	"$runner" "$@" > "benchmark-extras/results/$name/$i.json"
done

cd "benchmark-extras"

python3 data-eval.py "results/$name"
mv aggregated.csv "results/$name/$name.csv"
if [ -f cycles.csv ]; then
	mv cycles.csv "results/$name/cycles.csv"
fi
