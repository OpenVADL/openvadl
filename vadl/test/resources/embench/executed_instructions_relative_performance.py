import numpy as np
import pandas as pd
import sys
from scipy import stats

upstream = pd.read_csv(sys.argv[1], header=None)
lcb = pd.read_csv(sys.argv[2], header=None)

p = pd.merge(upstream,lcb,on=0)
p.columns = ['b', 'x', 'y']
p["rel"] = 1 + ((p['y'] - p['x']) / p['x'])

p.to_csv('../result/executed_instructions_relative.csv', header=None, index=False)
mean = stats.gmean(p["rel"])
print("Geometric mean of the relative performance is: " + str(mean))