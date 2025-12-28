#!/bin/bash

# Preserve exit code when piping to tee
set -o pipefail

# Workdir is /rtl, where the OpenVADL 'rtl' output is mounted
cd /rtl

# Translate Chisel to Verilog, writes the result to build/*
sbt "testOnly CoreEmit -- -z emit"

# Synthesize against Cmos cell library
yosys -s /scripts/bench/yosys/bench_spike_asic_cmos.ys | tee build/spike_asic_cmos.log

# Synthesize against SkyWater 130nm cell library
yosys -s /scripts/bench/yosys/bench_spike_asic_sky130.ys | tee build/spike_asic_sky130.log

