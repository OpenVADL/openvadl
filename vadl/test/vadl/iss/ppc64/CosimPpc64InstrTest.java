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

package vadl.iss.ppc64;

import java.io.IOException;
import java.math.BigInteger;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.iss.AsmTestBuilder;
import vadl.iss.CosimInstrTest;
import vadl.iss.CosimTestUtils;

public class CosimPpc64InstrTest extends CosimInstrTest {

  @Override
  public int getTestPerInstruction() {
    return 100;
  }

  @Override
  public String getVadlSpec() {
    return "sys/ppc64/ppc64.vadl";
  }

  @Override
  public AsmTestBuilder getBuilder(String testNamePrefix, int id) {
    return new Ppc64TestBuilder(testNamePrefix + id);
  }

  @Override
  public String compiler() {
    return "ppc64_compiler.py";
  }

  @Override
  public String cosimConfig() {
    return "ppc64_config.toml";
  }

  @Override
  public String withUpstreamTarget() {
    return "ppc64-softmmu";
  }

  // needed for all other tests
  @TestFactory
  Stream<DynamicTest> li() throws IOException {
    return testTRegImmS16Instruction("li", "LI");
  }

  /* not supported by compiler
  @TestFactory
  Stream<DynamicTest> addpcis() throws IOException {
    return testTRegImmS16Instruction("addpcis", "ADDPCIS");
  }
  */

  @TestFactory
  Stream<DynamicTest> add() throws IOException {
    return testTSSRegInstruction_OR("add", "ADD");
  }

  @TestFactory
  Stream<DynamicTest> addc() throws IOException {
    return testTSSRegInstruction_OR("addc", "ADDC");
  }

  @TestFactory
  Stream<DynamicTest> adde() throws IOException {
    return testTSSRegInstruction_OR("adde", "ADDE");
  }

  @TestFactory
  Stream<DynamicTest> subf() throws IOException {
    return testTSSRegInstruction_OR("subf", "SUBF");
  }

  @TestFactory
  Stream<DynamicTest> subfc() throws IOException {
    return testTSSRegInstruction_OR("subfc", "SUBFC");
  }

  @TestFactory
  Stream<DynamicTest> subfe() throws IOException {
    return testTSSRegInstruction_OR("subfe", "SUBFE");
  }

  @TestFactory
  Stream<DynamicTest> addme() throws IOException {
    return testTSRegInstruction_OR("addme", "ADDME");
  }

  @TestFactory
  Stream<DynamicTest> addze() throws IOException {
    return testTSRegInstruction_OR("addze", "ADDZE");
  }

  @TestFactory
  Stream<DynamicTest> subfme() throws IOException {
    return testTSRegInstruction_OR("subfme", "SUBFME");
  }

  @TestFactory
  Stream<DynamicTest> subfze() throws IOException {
    return testTSRegInstruction_OR("subfze", "SUBFZE");
  }

  @TestFactory
  Stream<DynamicTest> addi() throws IOException {
    return testTSRegImmS16Instruction("addi", "ADDI");
  }

  @TestFactory
  Stream<DynamicTest> addic() throws IOException {
    return testTSRegImmS16Instruction("addic", "ADDIC");
  }

  @TestFactory
  Stream<DynamicTest> addic_() throws IOException {
    return testTSRegImmS16Instruction("addic.", "ADDIC.");
  }

  @TestFactory
  Stream<DynamicTest> addis() throws IOException {
    return testTSRegImmS16Instruction("addis", "ADDIS");
  }

  @TestFactory
  Stream<DynamicTest> subfic() throws IOException {
    return testTSRegImmS16Instruction("subfic", "SUBFIC");
  }

  /* not yet implemented
  @TestFactory
  Stream<DynamicTest> addg6s() throws IOException {
    return testTSSRegInstruction("addg6s", "ADDG6S");
  }
  */

  /* not supported by compiler
  @TestFactory
  Stream<DynamicTest> addex() throws IOException {
    return testTSSRegInstructionExt("addex", "ADDEX", "0");
  }
  */

  @TestFactory
  Stream<DynamicTest> mulli() throws IOException {
    return testTSRegImmS16Instruction("mulli", "MULLI");
  }

  @TestFactory
  Stream<DynamicTest> mullw() throws IOException {
    return testTSSRegInstruction_OR("mullw", "MULLW");
  }

  @TestFactory
  Stream<DynamicTest> mulhw() throws IOException {
    return testTSSRegInstruction_R("mulhw", "MULHW");
  }

  @TestFactory
  Stream<DynamicTest> mulhwu() throws IOException {
    return testTSSRegInstruction_R("mulhwu", "MULHWU");
  }

  @TestFactory
  Stream<DynamicTest> divw() throws IOException {
    return testTSSRegInstruction_OR("divw", "DIVW");
  }

  @TestFactory
  Stream<DynamicTest> divwu() throws IOException {
    return testTSSRegInstruction_OR("divwu", "DIVWU");
  }

  /* not supported by compiler
  @TestFactory
  Stream<DynamicTest> divwe() throws IOException {
    return testTSSRegInstruction_OR("divwe", "DIVWE");
  }
  */

  /* not supported by compiler
  @TestFactory
  Stream<DynamicTest> divweu() throws IOException {
    return testTSSRegInstruction_OR("divweu", "DIVWEU");
  }
  */

  @TestFactory
  Stream<DynamicTest> neg() throws IOException {
    return testTSRegInstruction_OR("neg", "NEG");
  }

  /* not supported by compiler
  @TestFactory
  Stream<DynamicTest> modsw() throws IOException {
    return testTSSRegInstruction_OR("modsw", "MODSW");
  }
  */

  /* not supported by compiler
  @TestFactory
  Stream<DynamicTest> moduw() throws IOException {
    return testTSSRegInstruction_OR("moduw", "MODUW");
  }
  */

  @TestFactory
  Stream<DynamicTest> and() throws IOException {
    return testTSSRegInstruction_R("and", "AND");
  }

  @TestFactory
  Stream<DynamicTest> andc() throws IOException {
    return testTSSRegInstruction_R("andc", "ANDC");
  }

  @TestFactory
  Stream<DynamicTest> eqv() throws IOException {
    return testTSSRegInstruction_R("eqv", "EQV");
  }

  @TestFactory
  Stream<DynamicTest> nand() throws IOException {
    return testTSSRegInstruction_R("nand", "NAND");
  }

  @TestFactory
  Stream<DynamicTest> nor() throws IOException {
    return testTSSRegInstruction_R("nor", "NOR");
  }

  @TestFactory
  Stream<DynamicTest> or() throws IOException {
    return testTSSRegInstruction_R("or", "OR");
  }

  @TestFactory
  Stream<DynamicTest> orc() throws IOException {
    return testTSSRegInstruction_R("orc", "ORC");
  }

  @TestFactory
  Stream<DynamicTest> xor() throws IOException {
    return testTSSRegInstruction_R("xor", "XOR");
  }

  @TestFactory
  Stream<DynamicTest> crand() throws IOException {
    return testTSSCRBitInstruction("crand", "CRAND");
  }

  @TestFactory
  Stream<DynamicTest> crandc() throws IOException {
    return testTSSCRBitInstruction("crandc", "CRANDC");
  }

  @TestFactory
  Stream<DynamicTest> creqv() throws IOException {
    return testTSSCRBitInstruction("creqv", "CREQV");
  }

  @TestFactory
  Stream<DynamicTest> crnand() throws IOException {
    return testTSSCRBitInstruction("crnand", "CRNAND");
  }

  @TestFactory
  Stream<DynamicTest> crnor() throws IOException {
    return testTSSCRBitInstruction("crnor", "CRNOR");
  }

  @TestFactory
  Stream<DynamicTest> cror() throws IOException {
    return testTSSCRBitInstruction("cror", "CROR");
  }

  @TestFactory
  Stream<DynamicTest> crorc() throws IOException {
    return testTSSCRBitInstruction("crorc", "CRORC");
  }

  @TestFactory
  Stream<DynamicTest> crxor() throws IOException {
    return testTSSCRBitInstruction("crxor", "CRXOR");
  }

  @TestFactory
  Stream<DynamicTest> andi_() throws IOException {
    return testTSRegImmU16Instruction("andi.", "ANDI.");
  }

  @TestFactory
  Stream<DynamicTest> andis_() throws IOException {
    return testTSRegImmU16Instruction("andis.", "ANDIS.");
  }

  @TestFactory
  Stream<DynamicTest> ori() throws IOException {
    return testTSRegImmU16Instruction("ori", "ORI");
  }

  @TestFactory
  Stream<DynamicTest> oris() throws IOException {
    return testTSRegImmU16Instruction("oris", "ORIS");
  }

  @TestFactory
  Stream<DynamicTest> xori() throws IOException {
    return testTSRegImmU16Instruction("xori", "XORI");
  }

  @TestFactory
  Stream<DynamicTest> xoris() throws IOException {
    return testTSRegImmU16Instruction("xoris", "XORIS");
  }

  /* not supported by compiler
  @TestFactory
  Stream<DynamicTest> setb() throws IOException {
    return testTRegSCRFieldInstruction("setb", "SETB");
  }
  */

  /* not supported by compiler
  @TestFactory
  Stream<DynamicTest> setbc() throws IOException {
    return testTRegSCRBitInstruction("setbc", "SETBC");
  }
  */

  /* not supported by compiler
  @TestFactory
  Stream<DynamicTest> setbcr() throws IOException {
    return testTRegSCRBitInstruction("setbcr", "SETBCR");
  }
  */

  /* not supported by compiler
  @TestFactory
  Stream<DynamicTest> setnbc() throws IOException {
    return testTRegSCRBitInstruction("setnbc", "SETNBC");
  }
  */

  /* not supported by compiler
  @TestFactory
  Stream<DynamicTest> setnbcr() throws IOException {
    return testTRegSCRBitInstruction("setnbcr", "SETNBCR");
  }
  */

  @TestFactory
  Stream<DynamicTest> rlwimi() throws IOException {
    return testMFormRotateInstruction_R("rlwimi", "RLWIMI");
  }

  @TestFactory
  Stream<DynamicTest> rlwinm() throws IOException {
    return testMFormRotateInstruction_R("rlwinm", "RLWINM");
  }

  @TestFactory
  Stream<DynamicTest> rlwnm() throws IOException {
    return testMFormRotateInstruction_R("rlwnm", "RLWNM");
  }

  @TestFactory
  Stream<DynamicTest> slw() throws IOException {
    return testXFormShiftInstruction_R("slw", "SLW");
  }

  @TestFactory
  Stream<DynamicTest> srw() throws IOException {
    return testXFormShiftInstruction_R("srw", "SRW");
  }

  @TestFactory
  Stream<DynamicTest> sraw() throws IOException {
    return testXFormShiftInstruction_R("sraw", "SRAW");
  }

  @TestFactory
  Stream<DynamicTest> srawi() throws IOException {
    return testXFormShiftInstruction_R("srawi", "SRAWI");
  }

  @TestFactory
  Stream<DynamicTest> cmp() throws IOException {
    return testTCRFieldModeSSRegInstruction("cmp", "CMP");
  }

  @TestFactory
  Stream<DynamicTest> cmpl() throws IOException {
    return testTCRFieldModeSSRegInstruction("cmpl", "CMPL");
  }

  @TestFactory
  Stream<DynamicTest> cmpi() throws IOException {
    return testDFormCompareInstruction("cmpi", "CMPI");
  }

  @TestFactory
  Stream<DynamicTest> cmpli() throws IOException {
    return testDFormCompareInstruction("cmpli", "CMPLI");
  }

  /* not supported by compiler
  @TestFactory
  Stream<DynamicTest> cmpb() throws IOException {
    return testTSSRegInstruction("cmpb", "CMPB");
  }
  */

  /* not supported by compiler
  @TestFactory
  Stream<DynamicTest> cmpeqb() throws IOException {
    return testTCRFieldSSRegInstruction("cmpeqb", "CMPEQB");
  }
  */

  /* not supported by compiler
  @TestFactory
  Stream<DynamicTest> cmprb() throws IOException {
    return testTCRFieldModeSSRegInstruction("cmprb", "CMPRB");
  }
  */

  private Stream<DynamicTest> testTSSRegInstruction_OR(String instruction, String testNamePrefix)
      throws IOException {
    var s1 = testTSSRegInstruction(instruction, testNamePrefix);
    var s2 = testTSSRegInstruction(instruction + ".", testNamePrefix + ".");
    var s3 = testTSSRegInstruction(instruction + "o", testNamePrefix + "O");
    var s4 = testTSSRegInstruction(instruction + "o.", testNamePrefix + "O.");
    return Stream.concat(Stream.concat(s1, s2), Stream.concat(s3, s4));
  }

  private Stream<DynamicTest> testTSSRegInstruction_R(String instruction, String testNamePrefix)
      throws IOException {
    var s1 = testTSSRegInstruction(instruction, testNamePrefix);
    var s2 = testTSSRegInstruction(instruction + ".", testNamePrefix + ".");
    return Stream.concat(s1, s2);
  }

  private Stream<DynamicTest> testTSRegInstruction_OR(String instruction, String testNamePrefix)
      throws IOException {
    var s1 = testTSRegInstruction(instruction, testNamePrefix);
    var s2 = testTSRegInstruction(instruction + ".", testNamePrefix + ".");
    var s3 = testTSRegInstruction(instruction + "o", testNamePrefix + "O");
    var s4 = testTSRegInstruction(instruction + "o.", testNamePrefix + "O.");
    return Stream.concat(Stream.concat(s1, s2), Stream.concat(s3, s4));
  }

  private Stream<DynamicTest> testTSSRegInstruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regSrc1 = b.anyTempReg().sample();
      var regSrc2 = b.anyTempReg().sample();
      b.fillRegSigned(regSrc1, 16);
      b.fillRegSigned(regSrc2, 16);
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s, %s", instruction, regDest, regSrc1, regSrc2);
      return new CosimTestUtils.TestCase(testNamePrefix + id, b.toAsmString());
    });
  }

  private Stream<DynamicTest> testTSSRegInstructionExt(String instruction, String testNamePrefix, String ext)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regSrc1 = b.anyTempReg().sample();
      var regSrc2 = b.anyTempReg().sample();
      b.fillRegSigned(regSrc1, 16);
      b.fillRegSigned(regSrc2, 16);
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s, %s, %s", instruction, regDest, regSrc1, regSrc2, ext);
      return new CosimTestUtils.TestCase(testNamePrefix + id, b.toAsmString());
    });
  }

  private Stream<DynamicTest> testTCRFieldSSRegInstruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regSrc1 = b.anyTempReg().sample();
      var regSrc2 = b.anyTempReg().sample();
      b.fillRegSigned(regSrc1, 16);
      b.fillRegSigned(regSrc2, 16);
      b.add("%s %s, %s, %s", instruction, arbitraryCRField(), regSrc1, regSrc2);
      return new CosimTestUtils.TestCase(testNamePrefix + id, b.toAsmString());
    });
  }

  private Stream<DynamicTest> testTSSCRBitInstruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var bitSrc1 = arbitraryCRBit();
      var bitSrc2 = arbitraryCRBit();
      // TODO: fill CR with random values
      var bitDest = b.anyTempReg().sample();
      b.add("%s %s, %s, %s", instruction, bitDest, bitSrc1, bitSrc2);
      return new CosimTestUtils.TestCase(testNamePrefix + id, b.toAsmString());
    });
  }

  private Stream<DynamicTest> testTSRegImmS16Instruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regSrc = b.anyTempReg().sample();
      b.fillRegSigned(regSrc, 16);
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s, %s", instruction, regDest, regSrc, arbitraryImmS(16));
      return new CosimTestUtils.TestCase(testNamePrefix + id, b.toAsmString());
    });
  }

  private Stream<DynamicTest> testTSRegImmU16Instruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regSrc = b.anyTempReg().sample();
      b.fillRegSigned(regSrc, 16);
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s, %s", instruction, regDest, regSrc, arbitraryImmU(16));
      return new CosimTestUtils.TestCase(testNamePrefix + id, b.toAsmString());
    });
  }

  private Stream<DynamicTest> testTSRegInstruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regSrc = b.anyTempReg().sample();
      b.fillRegSigned(regSrc, 16);
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s", instruction, regDest, regSrc);
      return new CosimTestUtils.TestCase(testNamePrefix + id, b.toAsmString());
    });
  }

  private Stream<DynamicTest> testTRegSCRFieldInstruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var fieldSrc = arbitraryCRField();
      // TODO: fill CR with random values
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s", instruction, regDest, fieldSrc);
      return new CosimTestUtils.TestCase(testNamePrefix + id, b.toAsmString());
    });
  }

  private Stream<DynamicTest> testTRegSCRBitInstruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var bitSrc = arbitraryCRBit();
      // TODO: fill CR with random values
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s", instruction, regDest, bitSrc);
      return new CosimTestUtils.TestCase(testNamePrefix + id, b.toAsmString());
    });
  }

  private Stream<DynamicTest> testTRegImmS16Instruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s", instruction, regDest, arbitraryImmS(16));
      return new CosimTestUtils.TestCase(testNamePrefix + id, b.toAsmString());
    });
  }

  private Stream<DynamicTest> testDFormCompareInstruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regSrc = b.anyTempReg().sample();
      b.fillRegSigned(regSrc, 16);
      b.add("%s %s, %s, %s, %s", instruction, arbitraryCRField(), arbitraryImmU(1), regSrc, arbitraryImmS(16));
      return new CosimTestUtils.TestCase(testNamePrefix + id, b.toAsmString());
    });
  }

  private Stream<DynamicTest> testMFormRotateInstruction_R(String instruction, String testNamePrefix)
      throws IOException {
    var s1 = testMFormRotateInstruction(instruction, testNamePrefix);
    var s2 = testMFormRotateInstruction(instruction + ".", testNamePrefix + ".");
    return Stream.concat(s1, s2);
  }

  private Stream<DynamicTest> testMFormRotateInstruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regSrc = b.anyTempReg().sample();
      var regSrcOrImm5 = b.anyTempReg().sample();
      b.fillRegSigned(regSrc, 16);
      b.fillRegSigned(regSrcOrImm5, 16);
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s, %s, %s, %s", instruction, regDest, regSrc, regSrcOrImm5, arbitraryImmU(5), arbitraryImmU(5));
      return new CosimTestUtils.TestCase(testNamePrefix + id, b.toAsmString());
    });
  }

  private Stream<DynamicTest> testXFormShiftInstruction_R(String instruction, String testNamePrefix)
      throws IOException {
    var s1 = testXFormShiftInstruction(instruction, testNamePrefix);
    var s2 = testXFormShiftInstruction(instruction + ".", testNamePrefix + ".");
    return Stream.concat(s1, s2);
  }

  private Stream<DynamicTest> testXFormShiftInstruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regSrcOrImm5 = b.anyTempReg().sample();
      var regSrc= b.anyTempReg().sample();
      b.fillRegSigned(regSrcOrImm5, 16);
      b.fillRegSigned(regSrc, 16);
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s, %s", instruction, regDest, regSrcOrImm5, regSrc);
      return new CosimTestUtils.TestCase(testNamePrefix + id, b.toAsmString());
    });
  }

  private Stream<DynamicTest> testTCRFieldModeSSRegInstruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regSrc1 = b.anyTempReg().sample();
      var regSrc2 = b.anyTempReg().sample();
      b.fillRegSigned(regSrc1, 16);
      b.fillRegSigned(regSrc2, 16);
      b.add("%s %s, %s, %s, %s", instruction, arbitraryCRField(), arbitraryImmU(1), regSrc1, regSrc2);
      return new CosimTestUtils.TestCase(testNamePrefix + id, b.toAsmString());
    });
  }

  public static String arbitraryImmS(int bits) {
    var b = BigInteger.ONE.shiftLeft(bits - 1);
    return Arbitraries.bigIntegers()
        .greaterOrEqual(b.negate())
        .lessOrEqual(b.subtract(BigInteger.ONE))
        .sample()
        .toString();
  }

  public static String arbitraryImmU(int bits) {
    return Arbitraries.bigIntegers()
        .greaterOrEqual(BigInteger.ZERO)
        .lessOrEqual(BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE))
        .sample()
        .toString();
  }

  public static String arbitraryCRField() {
    return Arbitraries.integers()
        .between(0, 7)
        .map(String::valueOf)
        .sample();
  }

  public static String arbitraryCRBit() {
    return Arbitraries.integers()
        .between(0, 31)
        .map(String::valueOf)
        .sample();
  }

}
