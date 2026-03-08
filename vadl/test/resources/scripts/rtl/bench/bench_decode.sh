#!/bin/bash

# Preserve exit code when piping to tee
set -o pipefail

DECODE_MODULE="$1"

if [ -z "$DECODE_MODULE" ]; then
  echo "Usage: $0 <decode_module>"
  exit 1
fi

# Workdir is /rtl, where the OpenVADL 'rtl' output is mounted
cd /rtl

# Replace decode module name in scripts
sed -i "s/DECODE/${DECODE_MODULE}/g" /scripts/bench/yosys/bench_decode.ys
sed -i "s/DECODE/${DECODE_MODULE}/g" /scripts/bench/opensta/time_decode.tcl

# Translate Chisel to Verilog, writes the result to build/*
sbt "testOnly CoreEmit -- -z emit"

# Run the respective yosys synthesize scripts
yosys -s /scripts/bench/yosys/bench_decode.ys | tee build/synth_decode.log

# Timing analysis with OpenSTA
sta < /scripts/bench/opensta/time_decode.tcl | tee build/time_decode.log

