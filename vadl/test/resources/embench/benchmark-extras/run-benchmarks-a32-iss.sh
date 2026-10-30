#!/usr/bin/env bash
# SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

../build_virt-iss-a32.sh --cpu-mhz "$CPU_MHZ"
echo "Benchmarking open-vadl..."
./run-benchmark.sh "a32-open-vadl" ./benchmark_qemu.sh "qemu-system-a32" -nographic -bios
echo "Benchmarking qemu..."
./run-benchmark.sh "a32-qemu" ./benchmark_qemu.sh \
  "qemu-system-arm" -M virt -cpu cortex-a15 -m 4G -net none -nographic -semihosting -kernel
echo "Done."

python3 data-relative.py results-a32-iss \
        results/a32-qemu/a32-qemu.csv \
        results/a32-open-vadl/a32-open-vadl.csv
