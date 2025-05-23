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

package vadl.iss.aarch64;

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.iss.AsmTestBuilder;
import vadl.iss.IssTestUtils;

public class IssA64InstrTest extends AbstractIssAarch64InstrTest {
  @Override
  public int getTestPerInstruction() {
    return 0;
  }

  @Override
  public String getVadlSpec() {
    return "sys/aarch64/virt.vadl";
  }

  @Override
  public AsmTestBuilder getBuilder(String testNamePrefix, int id) {
    return new A64TestBuilder(testNamePrefix + id);
  }

  // TODO: Remove this once we have actual tests
  @TestFactory
  Stream<DynamicTest> simpleTest() throws IOException {
    var asmCore = """
        mov     x29, #2          // x29 = 2
        mov     x28, #4          // x28 = 3
        add     x29, x29, x28     // a0 = x29 + x28 (32-bit add)
        """;
    return runTest(new IssTestUtils.TestCase("Simple Test", asmCore));
  }
}
