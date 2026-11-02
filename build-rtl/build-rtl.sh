#!/bin/bash

# Prerequisites:
# * sbt
# * yosys
# * openroad

if ! command -v sbt >/dev/null 2>&1; then
    echo "Required command 'sbt' not found. Please install with 'brew install sbt'."
    exit 1
fi

if ! command -v yosys >/dev/null 2>&1; then
    echo "Required command 'yosys' not found. Please install with 'brew install yosys'."
    exit 1
fi

if ! command -v openroad >/dev/null 2>&1; then
    cat <<'EOF'
Required command 'openroad' not found.
OpenROAD can be built from source by following the instructions at https://openroad.readthedocs.io/en/latest/user/Build.html
or using the following steps:

> git clone --recursive https://github.com/The-OpenROAD-Project/OpenROAD.git
> cd OpenROAD
> ./etc/DependencyInstaller.sh -base
> ./etc/DependencyInstaller.sh -all
> ./etc/Build.sh

Add the directory containing the built openroad binary (*/bin) to PATH.
EOF
    exit 1
fi

VADL_CMD="/Users/matthias/Documents/uni/vadl/openvadl/vadl-cli/build/install/openvadl/bin/openvadl"
BASE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

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

# Initialize build directory
mkdir -p "$BASE_DIR/build"

# Output the hardware description next to the script
"$VADL_CMD" rtl "$SPEC" --reset-vector=reset_vector -o "$BASE_DIR/build"

# Prepare scripts
sed "s/TOP_MODULE/${TOP_MODULE}/g" "$BASE_DIR/yosys/bench_core_asic_sky130.ys" |
sed "s|/opt/pdk|${BASE_DIR}/pdk|g" \
  > "$BASE_DIR/build/rtl/yosys.ys"
sed "s/TOP_MODULE/${TOP_MODULE}/g;s/CLOCK_PERIOD/${CLOCK_PERIOD}/g" \
  "$BASE_DIR/openroad/bench_core_asic_sky130.tcl"  |
sed "s|/opt/pdk|${BASE_DIR}/pdk|g" |
sed "s|/rtl/build|${BASE_DIR}/build/rtl/build|g" \
  > "$BASE_DIR/build/rtl/openroad.tcl"

# Navigate to Chisel project directory
cd "$BASE_DIR/build/rtl"

# Translate Chisel to Verilog, writes the result to build/*
sbt "testOnly CoreEmit -- -t emit"

# Synthesize against SkyWater 130nm cell library
yosys -s ./yosys.ys | tee build/${TOP_MODULE}_yosys_asic_sky130.log

# Placement for more realistic area & timing estimates
openroad ./openroad.tcl | tee build/${TOP_MODULE}_openroad_asic_sky130.log