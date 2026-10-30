// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.iss.riscv;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import vadl.iss.IssDockerRuntime;
import vadl.iss.IssEmbenchBenchmark;

public class IssRiscvEmbenchBenchmarkTest extends IssEmbenchBenchmark {

  @Override
  protected IssDockerRuntime runtime() {
    return IssDockerRuntime.RISCV;
  }

  @Override
  protected List<String> withUpstreamTargets() {
    return List.of("riscv64-softmmu", "riscv32-softmmu");
  }

  @Tag("BenchmarkTest")
  @Test
  void rv64imBenchmarkTest() throws IOException {
    runBenchmark(benchmarkSpec(
        "rv64im",
        "sys/risc-v/rv64im.vadl",
        "benchmark-extras/run-benchmarks-rv64im.sh",
        "benchmark-extras/results-rv64im-iss"
    ));
  }

  @Tag("BenchmarkTest")
  @Test
  void rv64imdBenchmarkTest() throws IOException {
    runBenchmark(benchmarkSpec(
        "rv64imd",
        "sys/risc-v/rv64d.vadl",
        "benchmark-extras/run-benchmarks-rv64imd.sh",
        "benchmark-extras/results-rv64imd-iss"
    ));
  }

  @Tag("BenchmarkTest")
  @Test
  void rv32imBenchmarkTest() throws IOException {
    runBenchmark(benchmarkSpec(
        "rv32im",
        "sys/risc-v/rv32im.vadl",
        "benchmark-extras/run-benchmarks-rv32im.sh",
        "benchmark-extras/results-rv32im-iss"
    ));
  }
}
