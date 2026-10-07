#!/usr/bin/env bash

set -e
cd "$(dirname "$(realpath "$0")")"

# Never silently fall back to upstream QEMU in the integration test.
: "${QEMU_SYSTEM:?Set QEMU_SYSTEM to the generated ISS executable}"
: "${QEMU_MACHINE:?Set QEMU_MACHINE to the generated ISS machine}"

python3 ./benchmark_speed.py --json-output --absolute \
  --target-module run_sim "$(pwd)/__run_spike.sh"

