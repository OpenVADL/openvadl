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

# QEMU
../build_spike-rv64i.sh --cpu-mhz 500
echo "Benchmarking qemu..."
./run-benchmark.sh "rv64i-qemu"       ./benchmark_qemu.sh       "qemu-system-riscv64" -nographic -M spike -bios
echo "Benchmarking open-vadl..."
./run-benchmark.sh "rv64i-open-vadl"  ./benchmark_qemu.sh       "qemu-system-rv64i" -nographic -bios
echo "Done."

# Normalize dtc timings
python3 data-relative.py results-rv64i-iss \
        results/rv64i-qemu/rv64i-qemu.csv \
        results/rv64i-open-vadl/rv64i-open-vadl.csv

