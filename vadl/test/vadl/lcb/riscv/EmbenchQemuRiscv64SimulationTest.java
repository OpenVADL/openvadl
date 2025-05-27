// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
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

package vadl.lcb.riscv;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import vadl.lcb.LcbDockerExecutionTest;
import vadl.pass.exception.DuplicatedPassKeyException;

public class EmbenchQemuRiscv64SimulationTest extends LcbDockerExecutionTest {

  public String getTarget() {
    return "rv64im";
  }

  public String getUpstreamBuildTarget() {
    return "RISCV";
  }

  @Override
  protected String getUpstreamClangTarget() {
    return "riscv64";
  }

  @Override
  protected String getSpikeTarget() {
    return "rv64gc";
  }

  @Override
  protected String getAbi() {
    return "lp64";
  }

  @Test
  void runO0() throws DuplicatedPassKeyException, IOException {
    testEmbench(0);
  }

  @Test
  void runO3() throws DuplicatedPassKeyException, IOException {
    testEmbench(3);
  }

  void testEmbench(int optLevel) throws IOException, DuplicatedPassKeyException {
    var cmd = "sh /src/embench/benchmark-extras/rv64-run-benchmarks-spike-clang-lcb-O" + optLevel
        + ".sh";
    run("sys/risc-v/rv64im.vadl", cmd);
  }
}
