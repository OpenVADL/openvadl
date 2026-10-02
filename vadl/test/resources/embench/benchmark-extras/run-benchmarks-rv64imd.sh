#!/usr/bin/env bash
# SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
# SPDX-License-Identifier: GPL-3.0-or-later
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

set -e

cd $(realpath $(dirname "$0"))

CPU_MHZ="${EMBENCH_MHZ:-1000}"

# QEMU
../build_spike-rv64imd.sh --cpu-mhz "$CPU_MHZ"
echo "Benchmarking open-vadl..."
./run-benchmark.sh "rv64imd-open-vadl"  ./benchmark_qemu.sh       "qemu-system-rv64imd" -nographic -bios
echo "Benchmarking qemu..."
./run-benchmark.sh "rv64imd-qemu"       ./benchmark_qemu.sh       "qemu-system-riscv64" -nographic -M spike -bios
echo "Done."

# Normalize dtc timings
python3 data-relative.py results-rv64imd-iss \
        results/rv64imd-qemu/rv64imd-qemu.csv \
        results/rv64imd-open-vadl/rv64imd-open-vadl.csv
