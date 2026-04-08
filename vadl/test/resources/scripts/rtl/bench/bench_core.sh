#!/bin/bash

# Preserve exit code when piping to tee
set -o pipefail

TOP_MODULE="$1"
CLOCK_PERIOD="$2"
if [ -z "$CLOCK_PERIOD" ]; then
  CLOCK_PERIOD=5
fi

if [ -z "$TOP_MODULE" ]; then
  echo "Usage: $0 <top_module> [clock_period_in_ns]"
  exit 1
fi

# Workdir is /rtl, where the OpenVADL 'rtl' output is mounted
cd /rtl

# Prepare scripts
sed "s/TOP_MODULE/${TOP_MODULE}/g" /scripts/bench/yosys/bench_core_asic_sky130.ys \
  > /tmp/yosys.ys
sed "s/TOP_MODULE/${TOP_MODULE}/g;s/CLOCK_PERIOD/${CLOCK_PERIOD}/g" \
  /scripts/bench/openroad/bench_core_asic_sky130.tcl \
  > /tmp/openroad.tcl

# Translate Chisel to Verilog, writes the result to build/*
sbt "testOnly CoreEmit -- -z emit"

# Synthesize against SkyWater 130nm cell library
yosys -s /tmp/yosys.ys | tee build/${TOP_MODULE}_yosys_asic_sky130.log

# Placement for more realistic area & timing estimates
openroad /tmp/openroad.tcl | tee build/${TOP_MODULE}_openroad_asic_sky130.log