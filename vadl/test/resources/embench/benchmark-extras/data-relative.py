#!/usr/bin/env python3
import os
import sys
import pandas as pd

# Usage:
#   python3 ./data-relative.py output_dir baseline_result.csv next_result1.csv next_result2.csv ...
# Write relative timings in output_dir.

output_dir = sys.argv[1]
baseline = sys.argv[2]

os.makedirs(output_dir, exist_ok=True)

baseline_data = pd.read_csv(baseline, index_col="benchmark")

for f in sys.argv[2:]:
    data = pd.read_csv(f, index_col="benchmark", dtype={"iterations": "Int64"})
    data.time = data.time / baseline_data.time
    if "time_unit" in data.columns:
        data.time_unit = "ratio"
    with open(output_dir + "/" + os.path.basename(f), "w") as outfile:
        outfile.write(data.to_csv())
