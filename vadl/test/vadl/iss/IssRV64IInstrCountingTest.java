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

package vadl.iss;

import static vadl.TestUtils.arbitrarySignedInt;
import static vadl.TestUtils.arbitraryUnsignedInt;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the RV64I instruction but also counts the instructions executed.
 * It does so by checking insn_count QEMU register.
 * Keep in mind that the test framework has a special harness that hides additional
 * instructions so the expected insn_count is always larger than the instructions actually
 * executed.
 * See vadl/test/resources/scripts/iss_qemu/test_case_executer_v1.py
 */
public class IssRV64IInstrCountingTest extends QemuIssTest {
  private static final int TESTS_PER_INSTRUCTION = 1;
  private static final Logger log = LoggerFactory.getLogger(IssRV64IInstrCountingTest.class);

  @TestFactory
  Stream<DynamicTest> addi_count_ins() throws IOException {
    return runTestsWith(id -> {
      var b = new RV64ITestBuilder("ADDI_" + id);
      var aImm = arbitrarySignedInt(12).sample();
      var regSrc = b.anyTempReg().sample();
      b.add("addi %s, x0, %s", regSrc, aImm);

      var bImm = arbitrarySignedInt(12).sample();
      var regDest = b.anyTempReg().sample();
      b.add("addi %s, %s, %s", regDest, regSrc, bImm);
      return b.toTestSpecWithSpecialRegs(Map.of("insn_count", "0000000009"), regSrc, regDest);
    });
  }

  @TestFactory
  Stream<DynamicTest> addiw() throws IOException {
    return runTestsWith(id -> {
      var b = new RV64ITestBuilder("ADDIW_" + id);
      var aImm = arbitrarySignedInt(12).sample();
      var regSrc = b.anyTempReg().sample();
      b.add("addiw %s, x0, %s", regSrc, aImm);

      var bImm = arbitrarySignedInt(12).sample();
      var regDest = b.anyTempReg().sample();
      b.add("addiw %s, %s, %s", regDest, regSrc, bImm);
      return b.toTestSpecWithSpecialRegs(Map.of("insn_count", "0000000009"), regSrc, regDest);
    });
  }

  @TestFactory
  Stream<DynamicTest> lui() throws IOException {
    return runTestsWith((id) -> {
      var b = new RV64ITestBuilder("LUI_" + id);
      var destReg = b.anyTempReg().sample();
      var value = arbitraryUnsignedInt(20).sample();
      b.add("lui %s, %s", destReg, value);
      return b.toTestSpecWithSpecialRegs(Map.of("insn_count", "0000000008"), destReg);
    });
  }

  @SafeVarargs
  private Stream<DynamicTest> runTestsWith(
      Function<Integer, IssTestUtils.TestSpec>... generators) throws IOException {
    var image = generateCasSimulator("sys/risc-v/rv64im.vadl");
    var testCases = Stream.of(generators)
        .flatMap(genFunc -> IntStream.range(0, TESTS_PER_INSTRUCTION)
            .mapToObj(genFunc::apply)
        )
        .toList();
    return runQemuInstrTests(image, testCases);
  }
}
