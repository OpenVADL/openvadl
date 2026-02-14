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

package vadl.lcb.riscv.riscv32;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.TargetName;
import vadl.lcb.LcbDockerInputFileExecutionTest;
import vadl.pass.exception.DuplicatedPassKeyException;

public abstract class AsmGenRiscv32FileCheckTest extends LcbDockerInputFileExecutionTest {
  protected abstract String getComponent();

  @Override
  protected String getTarget() {
    return "rv32im";
  }

  @Override
  protected String getUpstreamBuildTarget() {
    return "RISCV";
  }

  @Override
  protected String getUpstreamClangTarget() {
    return "riscv32";
  }

  @Override
  protected String getSpikeTarget() {
    return "rv32im";
  }

  @Override
  protected String getAbi() {
    return "ilp32";
  }

  @Override
  protected String getImageName() {
    return "rv32im_asm_gen";
  }

  @Override
  protected LcbConfiguration getConfiguration() {
    return new LcbConfiguration(getConfiguration(false), new TargetName(getTarget()), true);
  }

  @TestFactory
  List<DynamicTest> testAsm() throws DuplicatedPassKeyException, IOException {
    return runEach("sys/risc-v/rv32imAsmGen.vadl",
        "test/resources/llvm/riscv/asm/rv32im/" + getComponent(),
        0,
        "sh /work/filecheck.sh");
  }
}
