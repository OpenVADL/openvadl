#!/bin/bash

# Preserve exit code when piping to tee
set -o pipefail

# Workdir is /rtl, where the OpenVADL 'rtl' output is mounted
cd /rtl

# Translate Chisel to Verilog, writes the result to build/*
sbt "testOnly CoreEmit -- -z emit"

# Run the respective yosys synthesize scripts
yosys -s /scripts/bench/yosys/bench_decode.ys | tee build/synth_decode.log

# Timing analysis with OpenSTA
sta < /scripts/bench/opensta/time_decode.tcl | tee build/time_decode.log

