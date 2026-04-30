// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.riscv;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestMethodOrder;
import vadl.iss.AsmTestBuilder;
import vadl.iss.IssTestUtils;

/**
 * Tests the RV64I instructions set.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IssRV64MInstrTest extends AbstractIssRiscv64InstrTest {


  @Override
  public int getTestPerInstruction() {
    return 50;
  }

  @Override
  public String getVadlSpec() {
    return "sys/risc-v/rv64v.vadl";
  }

  public AsmTestBuilder getBuilder(String testNamePrefix, int id) {
    return new RV64IMVTestBuilder(testNamePrefix + "_" + id);
  }

  @Test
  @Order(1)
  void setupInstructionTests() throws IOException {
    initializeInstructionBatchFromTestCases(this::buildInstructionTestCases, this::getInstructionName);
  }

  @TestFactory
  @Order(2)
  Stream<DynamicNode> buildInstructionTests() throws IOException {
    initializeInstructionBatchFromTestCases(this::buildInstructionTestCases, this::getInstructionName);
    return buildInstructionTestContainers();
  }

  // Helper methods
  private List<IssTestUtils.TestCase> buildInstructionTestCases() {
    var tests = new ArrayList<IssTestUtils.TestCase>();
    tests.addAll(testBinaryRegRegInstruction("mul", "MUL"));
    tests.addAll(testBinaryRegRegInstruction("mulh", "MULH"));
    tests.addAll(testBinaryRegRegInstruction("mulhu", "MULHU"));
    tests.addAll(testBinaryRegRegInstruction("mulhsu", "MULHSU"));
    tests.addAll(testBinaryRegRegInstruction("div", "DIV"));
    tests.addAll(testDivRemByCustom(10, "div", BigInteger.ZERO, "DIV_BY_ZERO"));
    tests.addAll(testDivRemByCustom(10, "div", BigInteger.ONE.negate(), "DIV_BY_MINUS_ONE"));
    tests.addAll(testBinaryRegRegInstruction("divu", "DIVU"));
    tests.addAll(testDivRemByCustom(10, "divu", BigInteger.ZERO, "DIVU_BY_ZERO"));
    tests.addAll(testDivRemByCustom(10, "divu", BigInteger.ONE.negate(), "DIVU_BY_MINUS_ONE"));
    tests.addAll(testBinaryRegRegInstruction("rem", "REM"));
    tests.addAll(testDivRemByCustom(10, "rem", BigInteger.ZERO, "REM_BY_ZERO"));
    tests.addAll(testDivRemByCustom(10, "rem", BigInteger.ONE.negate(), "REM_BY_MINUS_ONE"));
    tests.addAll(testBinaryRegRegInstruction("remu", "REMU"));
    tests.addAll(testDivRemByCustom(10, "remu", BigInteger.ZERO, "REMU_BY_ZERO"));
    tests.addAll(testDivRemByCustom(10, "remu", BigInteger.ONE.negate(), "REMU_BY_MINUS_ONE"));
    tests.addAll(testBinaryRegRegInstructionW("mulw", "MULW"));
    tests.addAll(testBinaryRegRegInstructionW("divw", "DIVW"));
    tests.addAll(testBinaryRegRegInstructionW("divuw", "DIVUW"));
    tests.addAll(testBinaryRegRegInstructionW("remw", "REMW"));
    tests.addAll(testBinaryRegRegInstructionW("remuw", "REMUW"));
    return tests;
  }

  private String getInstructionName(IssTestUtils.TestCase testCase) {
    var separator = testCase.id().lastIndexOf('_');
    if (separator <= 0) {
      return testCase.id();
    }
    return testCase.id().substring(0, separator).toLowerCase();
  }

  private List<IssTestUtils.TestCase> testBinaryRegRegInstruction(String instruction,
                                                                  String testNamePrefix) {
    return buildTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regSrc1 = b.anyTempReg().sample();
      var regSrc2 = b.anyTempReg().sample();
      b.fillRegSigned(regSrc1, 64);
      b.fillRegSigned(regSrc2, 64);
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s, %s", instruction, regDest, regSrc1, regSrc2);
      return b.toTestCase(regSrc1, regSrc2, regDest);
    });
  }

  private IssTestUtils.TestCase customBinaryRegRegInstr(String instr, BigInteger lhs,
                                                        BigInteger rhs,
                                                        AsmTestBuilder b) {
    var regSrc1 = b.anyTempReg().sample();
    var regSrc2 = b.anyTempReg().sample();
    b.fillReg(regSrc1, lhs);
    b.fillReg(regSrc2, rhs);
    var regDest = b.anyTempReg().sample();
    b.add("%s %s, %s, %s", instr, regDest, regSrc1, regSrc2);
    return b.toTestCase(regSrc1, regSrc2, regDest);
  }

  private List<IssTestUtils.TestCase> testDivRemByCustom(int runs, String instr, BigInteger divisor,
                                                         String testPrefix) {
    return buildTestsWith(runs, (i) -> {
      var b = getBuilder(testPrefix, i);
      var regSrc1 = b.anyTempReg().sample();
      var regSrc2 = b.anyTempReg().sample();
      b.fillRegSigned(regSrc1, 64);
      b.fillReg(regSrc2, divisor);
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s, %s", instr, regDest, regSrc1, regSrc2);
      return b.toTestCase(regSrc1, regSrc2, regDest);
    });
  }

  // Helper for 32-bit wide instructions (sign-extended)
  private List<IssTestUtils.TestCase> testBinaryRegRegInstructionW(String instruction,
                                                                   String testNamePrefix) {
    return buildTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regSrc1 = b.anyTempReg().sample();
      var regSrc2 = b.anyTempReg().sample();
      // Fill with 32-bit values so we test sign-ext
      b.fillRegSigned(regSrc1, 32);
      b.fillRegSigned(regSrc2, 32);
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s, %s", instruction, regDest, regSrc1, regSrc2);
      return b.toTestCase(regSrc1, regSrc2, regDest);
    });
  }

}
