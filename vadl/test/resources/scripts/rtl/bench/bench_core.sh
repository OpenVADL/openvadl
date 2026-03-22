#!/bin/bash

# Preserve exit code when piping to tee
set -o pipefail

TOP_MODULE="$1"

if [ -z "$TOP_MODULE" ]; then
  echo "Usage: $0 <top_module>"
  exit 1
fi

# Workdir is /rtl, where the OpenVADL 'rtl' output is mounted
cd /rtl

# Replace module name in scripts
sed -i "s/TOP_MODULE/${TOP_MODULE}/g" /scripts/bench/yosys/bench_core_asic_cmos.ys
sed -i "s/TOP_MODULE/${TOP_MODULE}/g" /scripts/bench/yosys/bench_core_asic_sky130.ys
sed -i "s/TOP_MODULE/${TOP_MODULE}/g" /scripts/bench/yosys/bench_core_fpga_ice40.ys

sed -i "s/TOP_MODULE/${TOP_MODULE}/g" /scripts/bench/opensta/time_core.tcl

# Translate Chisel to Verilog, writes the result to build/*
sbt "testOnly CoreEmit -- -z emit"

# Synthesize against Cmos cell library
yosys -s /scripts/bench/yosys/bench_core_asic_cmos.ys | tee build/core_asic_cmos.log

# Synthesize against SkyWater 130nm cell library
yosys -s /scripts/bench/yosys/bench_core_asic_sky130.ys | tee build/core_asic_sky130.log

# Synthesize against ice40 FPGA
yosys -s /scripts/bench/yosys/bench_core_fpga_ice40.ys | tee build/core_fpga_ice40.log

# Timing analysis with OpenSTA
sta < /scripts/bench/opensta/time_core.tcl | tee build/time_core.log

