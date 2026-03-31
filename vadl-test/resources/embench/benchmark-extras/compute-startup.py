#!/usr/bin/env python3
import os
import sys
import pandas as pd
import statsmodels.formula.api as sm

# expects the folders
#      results/aarch64-qemu-{1,10,50,100}
#      results/aarch64-qemu-nojit-{1,10,50,100}
#      results/aarch64-qemu-singlestep-{1,10,50,100}
#      results/aarch64-qemu-ume-{1,10,50,100}
#      results/aarch64-qemu-ume-nojit-{1,10,50,100}
#      results/aarch64-qemu-ume-singlestep-{1,10,50,100}

for t in ["aarch64-qemu","aarch64-qemu-nojit","aarch64-qemu-singlestep",
          "aarch64-qemu-ume","aarch64-qemu-ume-nojit",
          "aarch64-qemu-ume-singlestep"]:
	data = []
	for i in [1, 10, 50, 100]:
		d = pd.read_csv(f"results/{t}-{i}/{t}-{i}.csv")
		data.append([i, d.loc[d.benchmark=="mean",'time'].item()])

	df = pd.DataFrame(data, columns=['freq', 'time'])

	print(f"### {t}")
	print(df)
	print()

	# least mean squares
	# time = freq * t_benchmark + bias * t_startup
	df['bias'] = 1
	result = sm.ols(formula="time ~ freq + bias", data=df).fit()
	# print(f"time = cpu_freq * {round(result.params.freq, 2)} + {round(result.params.bias, 2)}")
	print("time per iteration", round(result.params.freq, 2))
	print("time for startup", round(result.params.bias, 2))

	print()

	# Naive version for plausability
	# for i in range(0, len(data) - 1):
	# 	for j in range(i+1, len(data)):
	# 		print("-",i,j)
	# 		n1, time1 = data[i]
	# 		n2, time2 = data[j]

	# 		t_benchmark = (time2-time1)/(n2-n1)
	# 		t_startup1 = time1 - (n1*t_benchmark)
	# 		t_startup2 = time2 - (n2*t_benchmark)
	# 		print(t_benchmark, t_startup1, " ; " , t_benchmark, t_startup2)

			# t_startup = ((time1/n1 - time2/n2)*n1*n2)/(n2-n1)
			# t_benchmark1 = (time1 - t_startup) / n1
			# t_benchmark2 = (time2 - t_startup) / n2
			# print(t_benchmark1, t_startup, " ; " , t_benchmark2, t_startup)
