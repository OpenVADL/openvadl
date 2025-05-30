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

package vadl.iss.riscv;

import java.io.IOException;
import java.math.BigInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.iss.AsmTestBuilder;
import vadl.iss.IssTestUtils;

/**
 * Tests the RV64I instructions set.
 */
public class IssRV64MInstrTest extends AbstractIssRiscv64InstrTest {


  @Override
  public int getTestPerInstruction() {
    return 50;
  }

  @Override
  public String getVadlSpec() {
    return "sys/risc-v/rv64im.vadl";
  }

  public AsmTestBuilder getBuilder(String testNamePrefix, int id) {
    return new RV64IMTestBuilder(testNamePrefix + "_" + id);
  }

  @TestFactory
  Stream<DynamicTest> mul() throws IOException {
    return testBinaryRegRegInstruction("mul", "MUL");
  }

  @TestFactory
  Stream<DynamicTest> mulh() throws IOException {
    return testBinaryRegRegInstruction("mulh", "MULH");
  }

  @TestFactory
  Stream<DynamicTest> mulhu() throws IOException {
    return testBinaryRegRegInstruction("mulhu", "MULHU");
  }

  @TestFactory
  Stream<DynamicTest> mulhsu() throws IOException {
    return testBinaryRegRegInstruction("mulhsu", "MULHSU");
  }

  @TestFactory
  Stream<DynamicTest> div() throws IOException {
    return testBinaryRegRegInstruction("div", "DIV");
  }

  @TestFactory
  Stream<DynamicTest> divByZero() throws IOException {
    return testDivRemByCustom(10, "div", BigInteger.ZERO, "DIV_BY_ZERO");
  }

  @TestFactory
  Stream<DynamicTest> divByMinusOne() throws IOException {
    return testDivRemByCustom(10, "div", BigInteger.ONE.negate(), "DIV_BY_MINUS_ONE");
  }

  @TestFactory
  Stream<DynamicTest> divu() throws IOException {
    return testBinaryRegRegInstruction("divu", "DIVU");
  }

  @TestFactory
  Stream<DynamicTest> divuByZero() throws IOException {
    return testDivRemByCustom(10, "divu", BigInteger.ZERO, "DIVU_BY_ZERO");
  }

  @TestFactory
  Stream<DynamicTest> divuByMinusOne() throws IOException {
    return testDivRemByCustom(10, "divu", BigInteger.ONE.negate(), "DIVU_BY_MINUS_ONE");
  }

  @TestFactory
  Stream<DynamicTest> rem() throws IOException {
    return testBinaryRegRegInstruction("rem", "REM");
  }

  @TestFactory
  Stream<DynamicTest> remByZero() throws IOException {
    return testDivRemByCustom(10, "rem", BigInteger.ZERO, "REM_BY_ZERO");
  }

  @TestFactory
  Stream<DynamicTest> remByMinusOne() throws IOException {
    return testDivRemByCustom(10, "rem", BigInteger.ONE.negate(), "REM_BY_MINUS_ONE");
  }

  @TestFactory
  Stream<DynamicTest> remu() throws IOException {
    return testBinaryRegRegInstruction("remu", "REMU");
  }

  @TestFactory
  Stream<DynamicTest> remuByZero() throws IOException {
    return testDivRemByCustom(10, "remu", BigInteger.ZERO, "REMU_BY_ZERO");
  }

  @TestFactory
  Stream<DynamicTest> remuByMinusOne() throws IOException {
    return testDivRemByCustom(10, "remu", BigInteger.ONE.negate(), "REMU_BY_MINUS_ONE");
  }

  @TestFactory
  Stream<DynamicTest> mulw() throws IOException {
    return testBinaryRegRegInstructionW("mulw", "MULW");
  }

  @TestFactory
  Stream<DynamicTest> divw() throws IOException {
    return testBinaryRegRegInstructionW("divw", "DIVW");
  }

  @TestFactory
  Stream<DynamicTest> divuw() throws IOException {
    return testBinaryRegRegInstructionW("divuw", "DIVUW");
  }

  @TestFactory
  Stream<DynamicTest> remw() throws IOException {
    return testBinaryRegRegInstructionW("remw", "REMW");
  }

  @TestFactory
  Stream<DynamicTest> remuw() throws IOException {
    return testBinaryRegRegInstructionW("remuw", "REMUW");
  }

  // Helper methods
  private Stream<DynamicTest> testBinaryRegRegInstruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
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

  private Stream<DynamicTest> testDivRemByCustom(int runs, String instr, BigInteger divisor,
                                                 String testPrefix)
      throws IOException {
    return runTestsWith(runs, (i) -> {
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
  private Stream<DynamicTest> testBinaryRegRegInstructionW(String instruction,
                                                           String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
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
