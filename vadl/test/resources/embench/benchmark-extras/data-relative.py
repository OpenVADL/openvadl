#!/usr/bin/env python3
import os
import sys
import pandas as pd

# Usage:
#   python3 ./data-relative.py output_dir baseline_result.csv next_result1.csv next_result2.csv ...
# Output is placed next to input files

output_dir = sys.argv[1]
baseline = sys.argv[2]

os.makedirs(output_dir, exist_ok=True)

baseline_data = pd.read_csv(baseline)

with open(output_dir + "/" + os.path.basename(baseline), "w") as outfile:
    baseline0 = baseline_data.copy()
    baseline0.time_mean = 1.0
    baseline0.time_min = 1.0
    baseline0["time_mean_speedup"] = 1.0
    baseline0["time_min_speedup"]  = 1.0
    outfile.write(baseline0.to_csv(index=False))

for f in sys.argv[2:]:
    data = pd.read_csv(f)
    data.time_mean = data.time_mean / baseline_data.time_mean
    data.time_min = data.time_min / baseline_data.time_min
    data["time_mean_speedup"] = 1 / data.time_mean
    data["time_min_speedup"]  = 1 / data.time_min
    data = data.round(2)
    with open(output_dir + "/" + os.path.basename(f), "w") as outfile:
        outfile.write(data.to_csv(index=False))
