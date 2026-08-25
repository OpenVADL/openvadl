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

package vadl.iss.passes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.AbstractTest;
import vadl.configuration.IssConfiguration;
import vadl.error.DiagnosticList;
import vadl.iss.passes.common.IssRegisterAccessLoweringPass;
import vadl.pass.PassOrders;

public class IssInfoRetrievalPassTest extends AbstractTest {

  @TestFactory
  Stream<DynamicTest> testTbState() {
    return Stream.of(
        invalid("invalid_tb_state_reg_too_big.vadl",
            "The number of execution state bits (64) may not exceed 32"),
        invalid("invalid_tb_state_multiple_reg_too_big.vadl",
            "The number of execution state bits (48) may not exceed 32"),
        invalid("invalid_duplicate_fe_flag.vadl", "There can only be one non sticky invalid flag")
    );
  }

  private DynamicTest invalid(String fileName, String diagSubstr) {
    return dynamicTest(fileName, () -> {
      var config = new IssConfiguration(getConfiguration(false));
      var diag = assertThrows(DiagnosticList.class, () -> setupPassManagerAndRunSpec(
          "passes/issInfoRetrieval/" + fileName,
          PassOrders.iss(config).untilFirst(IssRegisterAccessLoweringPass.class)
      ));
      assertEquals(1, diag.items.size());
      assertThat(diag.items.getFirst().getMessage(), containsString(diagSubstr));
    });
  }
}
