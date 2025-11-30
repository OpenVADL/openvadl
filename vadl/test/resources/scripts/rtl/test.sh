#!/bin/bash

cd /rtl

mkdir -p build
touch build/rv32i-riscv-tests.log

# Preserve sbt exit code when piping to tee
set -o pipefail

sbt test 2>&1 | tee build/rv32i-riscv-tests.log
