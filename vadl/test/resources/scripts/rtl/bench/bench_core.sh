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
sed -i "s/TOP_MODULE/${TOP_MODULE}/g" /scripts/bench/yosys/bench_core_asic_sky130.ys
sed -i "s/TOP_MODULE/${TOP_MODULE}/g" /scripts/bench/openroad/bench_core_asic_sky130.tcl

# Translate Chisel to Verilog, writes the result to build/*
sbt "testOnly CoreEmit -- -z emit"

# Synthesize against SkyWater 130nm cell library
yosys -s /scripts/bench/yosys/bench_core_asic_sky130.ys | tee build/yosys_asic_sky130.log

# Placement for more realistic area & timing estimates
openroad /scripts/bench/openroad/bench_core_asic_sky130.tcl | tee build/openroad_asic_sky130.log

# Reset module name in scripts
sed -i "s/${TOP_MODULE}/TOP_MODULE/g" /scripts/bench/yosys/bench_core_asic_sky130.ys
sed -i "s/${TOP_MODULE}/TOP_MODULE/g" /scripts/bench/openroad/bench_core_asic_sky130.tcl