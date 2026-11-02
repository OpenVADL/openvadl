#!/bin/bash

# Prerequisites:
# * sbt
# * yosys
# * openroad

VADL_CMD="/Users/matthias/Documents/uni/vadl/openvadl/vadl-cli/build/install/openvadl/bin/openvadl"

SPEC="$1"
if [ -z "$SPEC" ]; then
  echo "Usage: $0 <spec.vadl> <core_name> [clock_period_in_ns]"
  exit 1
fi

TOP_MODULE="$2"
if [ -z "$TOP_MODULE" ]; then
  echo "Usage: $0 <spec.vadl> <core_name> [clock_period_in_ns]"
  exit 1
fi

CLOCK_PERIOD="$3"
if [ -z "$CLOCK_PERIOD" ]; then
  CLOCK_PERIOD=5
fi

# Initialize directory for benchmark artefacts
mkdir -p build

# Output the hardware description next to the script
"$VADL_CMD" rtl "$SPEC" --reset-vector=reset_vector -o build

# Prepare scripts
sed "s/TOP_MODULE/${TOP_MODULE}/g" ./yosys/bench_core_asic_sky130.ys \
  > ./build/rtl/yosys.ys
sed "s/TOP_MODULE/${TOP_MODULE}/g;s/CLOCK_PERIOD/${CLOCK_PERIOD}/g" \
  openroad/bench_core_asic_sky130.tcl \
  > ./build/rtl/openroad.tcl

# Navigate to Chisel project dir
cd build/rtl

# Translate Chisel to Verilog, writes the result to build/*
if [ ! -f "build/${TOP_MODULE}.sv" ]; then
  sbt "testOnly CoreEmit -- -z emit"
fi

# Synthesize against SkyWater 130nm cell library
yosys -s ./yosys.ys | tee build/${TOP_MODULE}_yosys_asic_sky130.log

# Placement for more realistic area & timing estimates
openroad ./openroad.tcl | tee build/${TOP_MODULE}_openroad_asic_sky130.log