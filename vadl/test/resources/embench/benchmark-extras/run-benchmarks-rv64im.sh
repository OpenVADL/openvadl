#!/usr/bin/env bash
set -e

cd $(realpath $(dirname "$0"))

# QEMU
../build_spike-rv64im.sh --cpu-mhz 1
echo "Benchmarking open-vadl..."
which qemu-system-rv64im
./run-benchmark.sh "rv64im-open-vadl"  ./benchmark_qemu.sh       "qemu-system-rv64im" -nographic -M virt -bios

echo "Benchmarking qemu..."
which qemu-system-riscv64
./run-benchmark.sh "rv64im-qemu"       ./benchmark_qemu.sh       "qemu-system-riscv64" -nographic -M spike -bios
echo "Done."

# Normalize dtc timings
python3 data-relative.py results-rv64im-iss \
        results/rv64im-qemu/rv64im-qemu.csv \
        results/rv64im-open-vadl/rv64im-open-vadl.csv
