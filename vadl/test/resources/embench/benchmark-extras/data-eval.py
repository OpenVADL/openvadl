#!/usr/bin/env python3
import json
import math
import os
import sys
from pathlib import Path

import pandas as pd


def handle_speed_benchmark():
    directory = Path(sys.argv[1])
    metadata = json.loads((directory / "build-metadata.json").read_text())
    iterations = metadata["iterations"]
    trials = []
    for filename in sorted(directory.glob("[0-9]*.json")):
        print("Loading", filename)
        detailed = json.loads(filename.read_text())["speed results"]["detailed speed results"]
        if not detailed or set(detailed) != set(iterations):
            raise ValueError(f"Benchmark set does not match the build metadata: {filename}")
        if any(not isinstance(value, (int, float)) or isinstance(value, bool)
               or not math.isfinite(value) or value <= 0 for value in detailed.values()):
            raise ValueError(f"Timings must be finite positive numbers: {filename}")
        trials.append(detailed)
    if not trials:
        raise ValueError(f"No trial timings found in {directory}")

    minima = pd.DataFrame(trials).min().sort_index()
    data = minima.to_frame(name="time")
    data["statistic"] = "min"
    data["iterations"] = pd.Series(iterations, dtype="Int64")
    data.loc["mean", "time"] = math.exp(math.fsum(math.log(value) for value in minima) / len(minima))
    data.loc["mean", "statistic"] = "geometric_mean"
    data["trials"] = len(trials)
    data["cpu_mhz"] = metadata["cpu_mhz"]
    data["warmup_iterations"] = metadata["warmup_iterations"]
    data["time_unit"] = "ms"
    data.to_csv("aggregated.csv", index_label="benchmark")


def handle_cycles():
    directory = "../bd/src"
    output_name = "cycles.csv"
    data = dict()
    for dir in sorted(os.listdir(directory)):
        f = os.path.join(directory, dir, "cycles.txt")
        if not os.path.exists(f):
            return
        with open(f) as file:
            cycles = int(file.read())
            data[dir] = cycles
    data = pd.Series(data=data)
    result = data.to_csv(header=["cycles"], index_label="benchmark")
    with open(output_name, "w") as outfile:
        outfile.write(result)


if __name__ == "__main__":
    handle_speed_benchmark()
    handle_cycles()
