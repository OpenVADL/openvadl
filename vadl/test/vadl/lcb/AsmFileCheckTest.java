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

package vadl.lcb;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.pass.exception.DuplicatedPassKeyException;

public abstract class AsmFileCheckTest extends LcbDockerInputFileExecutionTest {
  protected abstract String getSpecPath();
  protected abstract String getComponent();

  @TestFactory
  List<DynamicTest> testAsm() throws DuplicatedPassKeyException, IOException {
    return runEach(getSpecPath(),
        "test/resources/llvm/riscv/asm/" + getComponent(),
        0,
        "sh /work/filecheck.sh");
  }
}
