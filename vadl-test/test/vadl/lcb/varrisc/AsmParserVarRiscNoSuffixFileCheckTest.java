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

package vadl.lcb.varrisc;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.TargetName;
import vadl.lcb.LcbDockerInputFileExecutionTest;
import vadl.pass.exception.DuplicatedPassKeyException;

public class AsmParserVarRiscNoSuffixFileCheckTest extends LcbDockerInputFileExecutionTest {
  @Override
  protected String getTarget() {
    return "varrisc";
  }

  @Override
  protected String getUpstreamBuildTarget() {
    return "";
  }

  @Override
  protected String getUpstreamClangTarget() {
    return "varrisc";
  }

  @Override
  protected String getSpikeTarget() {
    return "varrisc";
  }

  @Override
  protected String getAbi() {
    return "ilp32";
  }

  @Override
  protected String getImageName() {
    return "varrisc_no_suffix";
  }

  @Override
  protected LcbConfiguration getConfiguration() {
    return new LcbConfiguration(getConfiguration(false), new TargetName(getTarget()), true);
  }

  private void copyFileCheckScript(LcbConfiguration configuration) throws IOException {
    var inputStream = new FileInputStream(
        "test/resources/images/lcb_execution_test_" + getTarget() + "/filecheck.sh");
    var outputStream =
        new FileOutputStream(configuration.outputPath() + "/lcb/filecheck.sh");
    inputStream.transferTo(outputStream);
    outputStream.close();
  }

  @Override
  protected void copyIntoDockerContext(LcbConfiguration configuration) throws IOException {
    super.copyIntoDockerContext(configuration);
    copyFileCheckScript(configuration);
  }

  @TestFactory
  List<DynamicTest> testAsm() throws DuplicatedPassKeyException, IOException {
    return runEach("sys/v-risc/ABI_no_suffix.vadl",
        "test/resources/llvm/varrisc/asm_no_suffix",
        0,
        "sh /work/filecheck.sh");
  }
}
