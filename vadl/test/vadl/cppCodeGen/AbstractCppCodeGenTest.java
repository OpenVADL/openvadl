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

package vadl.cppCodeGen;

import java.io.IOException;
import vadl.DockerExecutionTest;
import vadl.configuration.GcbConfiguration;
import vadl.gcb.valuetypes.TargetName;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;

public class AbstractCppCodeGenTest extends DockerExecutionTest {

  @Override
  public GcbConfiguration getConfiguration(boolean doDump) {
    return new GcbConfiguration(super.getConfiguration(doDump),
        new TargetName("processorNameValue"));
  }

  public record TestCase(String testName, String code) {

  }

  /**
   * Runs gcb passorder.
   */
  public TestSetup runGcbAndCppCodeGen(GcbConfiguration configuration,
                                       String specPath)
      throws IOException, DuplicatedPassKeyException {
    return setupPassManagerAndRunSpec(specPath, PassOrders.gcbAndCppCodeGen(configuration));
  }
}
