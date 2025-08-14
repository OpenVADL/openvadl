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

package vadl.lcb.aarch64;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.lcb.LcbDockerInputFileExecutionTest;
import vadl.pass.exception.DuplicatedPassKeyException;

@Disabled
public class QemuAarch64SimulationTest extends LcbDockerInputFileExecutionTest {
  @Override
  protected String getTarget() {
    return "aarch64temp";
  }

  protected String getSpecPath() {
    return "sys/aarch64/aarch64-abi.vadl";
  }

  @Override
  protected String getUpstreamBuildTarget() {
    return "AArch64";
  }

  @Override
  protected String getUpstreamClangTarget() {
    return "aarch64";
  }

  @Override
  protected String getSpikeTarget() {
    return "aarch64";
  }

  @Override
  protected String getAbi() {
    return "lp64";
  }

  @TestFactory
  List<DynamicTest> optLevel0() throws DuplicatedPassKeyException, IOException {
    return runEach(getSpecPath(),
        List.of("test/resources/llvm/riscv/qemu_nolibc/"
        ),
        0,
        "sh /work/compile.sh");
  }

  @TestFactory
  List<DynamicTest> optLevel3() throws DuplicatedPassKeyException, IOException {
    return runEach(getSpecPath(),
        List.of("test/resources/llvm/riscv/qemu_nolibc/"),
        3,
        "sh /work/compile.sh");
  }
}
