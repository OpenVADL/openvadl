#!/usr/bin/env bash
set -e

cd $(realpath $(dirname "$0"))

# Contains all simulators to use for benchmarking
# Array contains pairs of a name and the absolute path to the simulator binary
# The name will be used by the run-benchmark.sh script
VADL_SIMS=(
	p1 $(realpath ../bd-vadl/vadl-cas-rv32i_m_p1)
	p2 $(realpath ../bd-vadl/vadl-cas-rv32i_m_p2)
	p3 $(realpath ../bd-vadl/vadl-cas-rv32i_m_p3)
	p5 $(realpath ../bd-vadl/vadl-cas-rv32i_m_p5)
	hdl-p3 $(realpath ../bd-vadl/vadl-hdl-rv32i_m_p3)
	# hdl-p5 $(realpath ../bd-vadl/vadl-hdl-rv32i_m_p5) # takes more than 60s
)

QEMU_PREFIX=/opt/qemu/bin
QEMU_NOJIT_PREFIX=/opt/qemu-nojit/bin # compiled with --enable-tcg-interpreter
GEM5_BIN=gem5.fast # $(realpath ~/gem5/build/ALL/gem5.opt)

# QEMU
../build_rv32im.sh --cpu-mhz 100
./run-benchmark.sh "rv32-qemu"                  ./benchmark_qemu.sh       "$QEMU_PREFIX/qemu-riscv32"
./run-benchmark.sh "rv32-qemu-singlestep"       ./benchmark_qemu.sh       "$QEMU_PREFIX/qemu-riscv32" -one-insn-per-tb
./run-benchmark.sh "rv32-qemu-nojit"            ./benchmark_qemu.sh "$QEMU_NOJIT_PREFIX/qemu-riscv32"

# Gem5
../build_rv32im.sh
./run-benchmark.sh "rv32-gem5-atomic" ./benchmark_gem5.sh "$GEM5_BIN" "$(realpath ../gem5-scripts/atomic-rv32.py)"
# ./run-benchmark.sh "rv32-gem5-timing" ./benchmark_gem5.sh "$GEM5_BIN" "$(realpath ../gem5-scripts/timing-rv32.py)"

# VADL DTC
../build_vadl_rv32.sh --cpu-mhz 100
./run-benchmark.sh "rv32-dtc" ./benchmark_vadl.sh $(realpath ../bd-vadl/vadl-dtc-rv32i_m)

# VADL CAS/HDL
../build_vadl_rv32.sh
for i in $(seq 0 2 $((${#VADL_SIMS[@]}-1))); do
	NAME=${VADL_SIMS[i]}
	SIM=${VADL_SIMS[i + 1]}
	./run-benchmark.sh "rv32-${NAME}" ./benchmark_vadl.sh ${SIM}
done

# Spike
../build_spike.sh --cpu-mhz 100
./run-benchmark.sh "rv32-spike" ./benchmark_spike.sh

# Normalize dtc timings
python3 data-relative.py results-rv32-dtc results/rv32-qemu/rv32-qemu.csv \
        results/rv32-dtc/rv32-dtc.csv \
        results/rv32-qemu-singlestep/rv32-qemu-singlestep.csv \
        results/rv32-qemu-nojit/rv32-qemu-nojit.csv \
        results/rv32-spike/rv32-spike.csv

# Normalize cas timings
python3 data-relative.py results-rv32-cas \
        results/rv32-p1/rv32-p1.csv \
        results/rv32-p2/rv32-p2.csv \
        results/rv32-p3/rv32-p3.csv \
        results/rv32-p5/rv32-p5.csv \
        results/rv32-gem5-atomic/rv32-gem5-atomic.csv

# Copy cycles into output folder
cp results/rv32-p1/cycles.csv           results-rv32-cas/rv32-p1-cycles.csv
cp results/rv32-p2/cycles.csv           results-rv32-cas/rv32-p2-cycles.csv
cp results/rv32-p3/cycles.csv           results-rv32-cas/rv32-p3-cycles.csv
cp results/rv32-p5/cycles.csv           results-rv32-cas/rv32-p5-cycles.csv
cp results/rv32-gem5-atomic/cycles.csv  results-rv32-cas/rv32-gem5-atomic-cycles.csv

# Generate CAS "order of magnitude" comparison
echo "simulator,time" > results-rv32-cas/verilator.csv
cat results/rv32-hdl-p3/rv32-hdl-p3.csv          | grep mean | awk -F "," '{printf "hdl-p3,";      print $2}'     >>results-rv32-cas/verilator.csv
cat results/rv32-p3/rv32-p3.csv                  | grep mean | awk -F "," '{printf "rv32-p3,";     print $2}'     >>results-rv32-cas/verilator.csv
cat results/rv32-gem5-atomic/rv32-gem5-atomic.csv| grep mean | awk -F "," '{printf "gem5-atomic,"; print $2}'     >>results-rv32-cas/verilator.csv
cat results/rv32-dtc/rv32-dtc.csv                | grep mean | awk -F "," '{printf "rv32-dtc,";    print $2/100}' >>results-rv32-cas/verilator.csv
