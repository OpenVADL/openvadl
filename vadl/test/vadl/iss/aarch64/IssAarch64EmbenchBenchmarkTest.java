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

package vadl.iss.aarch64;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import vadl.iss.IssEmbenchBenchmark;

public class IssAarch64EmbenchBenchmarkTest extends IssEmbenchBenchmark {

  @Override
  protected List<String> withUpstreamTargets() {
    return List.of("aarch64-softmmu");
  }

  @Tag("BenchmarkTest")
  @Test
  void a64BenchmarkTest() throws IOException {
    runBenchmark(benchmarkSpec(
        "a64",
        "sys/aarch64/virt.vadl",
        "benchmark-extras/run-benchmarks-a64-iss.sh",
        "benchmark-extras/results-a64-iss"
    ));
  }
}
