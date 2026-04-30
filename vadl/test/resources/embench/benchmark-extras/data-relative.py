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
    baseline0.time = 1
    outfile.write(baseline0.to_csv(index=False))

for f in sys.argv[2:]:
    data = pd.read_csv(f)
    data.time = data.time / baseline_data.time
    with open(output_dir + "/" + os.path.basename(f), "w") as outfile:
        outfile.write(data.to_csv(index=False))
