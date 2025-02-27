#!/usr/bin/env bash
set -e

cd $(realpath $(dirname "$0"))

# QEMU
../build_spike-rv64i.sh --cpu-mhz 500
echo "Benchmarking qemu..."
./run-benchmark.sh "rv64i-qemu"       ./benchmark_qemu.sh       "qemu-system-riscv64" -nographic -M spike -bios
echo "Benchmarking open-vadl..."
./run-benchmark.sh "rv64i-open-vadl"  ./benchmark_qemu.sh       "qemu-system-rv64i" -nographic -M virt -bios
echo "Done."

# Normalize dtc timings
python3 data-relative.py results-rv64i-iss \
        results/rv64i-qemu/rv64i-qemu.csv \
        results/rv64i-open-vadl/rv64i-open-vadl.csv

