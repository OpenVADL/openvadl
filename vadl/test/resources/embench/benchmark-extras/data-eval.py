#!/usr/bin/env python3
import os
import sys
import pandas as pd
import numpy as np
from scipy.stats import gmean


def handle_speed_benchmark():
    directory = sys.argv[1]
    output_name = "aggregated.csv"

    input = list()
    for filename in os.listdir(directory):
        f = os.path.join(directory, filename)
        print("Loading", f)
        data = pd.read_json(f)
        detailed = data.loc[["detailed speed results"], "speed results"].values[0]
        detailed = pd.DataFrame.from_dict(detailed, orient="index", columns=["speed results"])
        detailed = detailed.transpose()

        data.drop(["detailed speed results"], inplace=True)
        data = data.transpose()
        data = pd.concat([detailed, data], axis=1)
        input.append(data)

    data = pd.concat(input)
    # Remove mean
    data = data.drop(["speed geometric standard deviation"], axis=1)
    data = data.drop(["speed geometric mean"], axis=1)
    data = pd.DataFrame({"mean": data.mean(), "min": data.min().astype(float)})
    geo_means = gmean(data, axis=0)
    data.loc['geomean'] = geo_means
    data = data.round(2)

    result = data.to_csv(header=["time_mean", "time_min"], index_label="benchmark")
    with open(output_name, "w") as outfile:
        outfile.write(result)


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


handle_speed_benchmark()
handle_cycles()
