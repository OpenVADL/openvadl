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

package vadl.viam.passes;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.AbstractTest;
import vadl.error.DiagnosticList;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;

public class DuplicateWriteDetectionPassTest extends AbstractTest {

  static Stream<Arguments> invalidTestArgs() {
    var regErrMsg = "Register is written twice at same index";
    var memErrMsg = "Memory address is written twice";
    return Stream.of(
        of("reg_single_branch", 1, regErrMsg),
        of("reg_dual_branch", 1, regErrMsg),
        of("reg_triple_branch", 1, regErrMsg),
        of("reg_potential_branch", 1, regErrMsg),
        of("regfile_1", 1, regErrMsg),
        of("regfile_2", 1, regErrMsg),
        of("regfile_3", 2, regErrMsg),
        of("regfile_4", 1, regErrMsg),
        of("mem_1", 1, memErrMsg),
        of("mem_2", 1, memErrMsg),
        of("mem_3", 2, memErrMsg),
        of("mem_4", 1, memErrMsg)

    );
  }

  static Stream<Arguments> validTestArgs() {
    var tests = AbstractTest.findAllTestSources("passes/singleResourceWriteValidation/valid_");
    return tests.stream().map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("invalidTestArgs")
  void testInvalid(String name, int numErrs, String errmsg)
      throws IOException, DuplicatedPassKeyException {

    var err = assertThrows(DiagnosticList.class, () -> setupPassManagerAndRunSpec(
        "passes/singleResourceWriteValidation/invalid_" + name + ".vadl",
        PassOrders.viam(getConfiguration(false))
            .untilFirst(DuplicateWriteDetectionPass.class)
    ));

    assertEquals(numErrs, err.items.size());
    for (var item : err.items) {
      assertThat(item.getMessage(), containsString(errmsg));
    }
  }


  @ParameterizedTest
  @MethodSource("validTestArgs")
  void validTest(String test) throws IOException, DuplicatedPassKeyException {
    setupPassManagerAndRunSpec(
        test,
        PassOrders.viam(getConfiguration(false))
            .untilFirst(DuplicateWriteDetectionPass.class)
            .addDump("test-weird")
    );
  }

}
