import argparse
import pandas as pd

parser = argparse.ArgumentParser(description='Compare upstream and LCB benchmark measurements')
parser.add_argument('upstream')
parser.add_argument('lcb')
parser.add_argument('--output', default='../result/executed_instructions_relative.csv')
parser.add_argument('--metric', default='executed instructions')
args = parser.parse_args()

upstream = pd.read_csv(args.upstream, header=None)
lcb = pd.read_csv(args.lcb, header=None)

p = pd.merge(upstream,lcb,on=0)
p.columns = ['b', 'upstream', 'lcb']
p["rel"] = p['upstream'] / p['lcb']

p.to_csv(args.output, header=None, index=False)
mean = p["rel"].mean()
print("Upstream / LCB")
print(args.metric + " arithmetic mean: " + str(mean) + "(" + str(1 - mean) + ")")
if p['b'].eq('crc_dse').any():
    print("WARNING: This mean includes crc_dse, which is not part of Embench. "
          "Exclude crc_dse when calculating the mean reported in a paper.")
