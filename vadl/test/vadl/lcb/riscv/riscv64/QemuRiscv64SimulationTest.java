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

package vadl.lcb.riscv.riscv64;

import java.util.stream.Stream;
import vadl.lcb.riscv.QemuRiscvSimulationTest;

public class QemuRiscv64SimulationTest extends QemuRiscvSimulationTest {
  @Override
  protected String getTarget() {
    return "rv64im";
  }

  @Override
  protected String getSpecPath() {
    return "sys/risc-v/rv64im.vadl";
  }

  @Override
  protected String getSpikeTarget() {
    return "rv64gc";
  }

  @Override
  protected String getAbi() {
    return "lp64";
  }

  @Override
  protected Stream<String> inputFilesFromCFile() {
    // Read the base tests + the tests under rv64im
    // The tests copy all the tests into the container, but we do need to update
    // the file name so it points to correct subdirectory, since every file is only executed
    // per container, and it is controlled over the "INPUT" environment variable.
    return Stream.concat(super.inputFilesFromCFile(),
        readFiles(TEST_RESOURCES_LLVM_RISCV_SPIKE + "/" + getTarget())
            .map(x -> getTarget() + "/" + x));
  }

  @Override
  protected String getUpstreamBuildTarget() {
    return "RISCV";
  }

  @Override
  protected String getUpstreamClangTarget() {
    return "riscv64";
  }
}
