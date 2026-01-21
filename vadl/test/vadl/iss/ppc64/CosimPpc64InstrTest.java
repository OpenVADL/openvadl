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

package vadl.iss.ppc64;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestMethodOrder;

/* Tests ppc64.vadl instructions against the QEMU ppc64 simulator.
 * Some instructions are defined in the VADL specification but are not covered by these tests:
 *   mfmsr, mtmsr, mfspr, mtspr, mftb and all branch instructions.
 * Load/Store instructions are tested with a reduced address space:
 *   0x0000'0000'0000'1000 - 0x0000'0000'0000'FFFF
 */

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CosimPpc64InstrTest extends AbstractCosimPpc64InstrTest {

  private static final long BASE_ADDRESS_LOAD_STORE = 0x1000L;

  @TestFactory
  @Order(1)
  Stream<DynamicTest> addpcis() throws IOException {
    return testTRegImmS16Instruction("addpcis", "ADDPCIS");
  }

  @TestFactory
  @Order(2)
  Stream<DynamicTest> add() throws IOException {
    return testTSSRegInstruction_OR("add", "ADD");
  }

  @TestFactory
  @Order(3)
  Stream<DynamicTest> addc() throws IOException {
    return testTSSRegInstruction_OR("addc", "ADDC");
  }

  @TestFactory
  @Order(4)
  Stream<DynamicTest> adde() throws IOException {
    return testTSSRegInstruction_OR("adde", "ADDE");
  }

  @TestFactory
  @Order(5)
  Stream<DynamicTest> subf() throws IOException {
    return testTSSRegInstruction_OR("subf", "SUBF");
  }

  @TestFactory
  @Order(6)
  Stream<DynamicTest> subfc() throws IOException {
    return testTSSRegInstruction_OR("subfc", "SUBFC");
  }

  @TestFactory
  @Order(7)
  Stream<DynamicTest> subfe() throws IOException {
    return testTSSRegInstruction_OR("subfe", "SUBFE");
  }

  @TestFactory
  @Order(8)
  Stream<DynamicTest> addme() throws IOException {
    return testTSRegInstruction_OR("addme", "ADDME");
  }

  @TestFactory
  @Order(9)
  Stream<DynamicTest> addze() throws IOException {
    return testTSRegInstruction_OR("addze", "ADDZE");
  }

  @TestFactory
  @Order(10)
  Stream<DynamicTest> subfme() throws IOException {
    return testTSRegInstruction_OR("subfme", "SUBFME");
  }

  @TestFactory
  @Order(11)
  Stream<DynamicTest> subfze() throws IOException {
    return testTSRegInstruction_OR("subfze", "SUBFZE");
  }

  @TestFactory
  @Order(12)
  Stream<DynamicTest> addi() throws IOException {
    return testTSRegImmS16Instruction("addi", "ADDI");
  }

  @TestFactory
  @Order(13)
  Stream<DynamicTest> addic() throws IOException {
    return testTSRegImmS16Instruction_R("addic", "ADDIC");
  }

  @TestFactory
  @Order(14)
  Stream<DynamicTest> addis() throws IOException {
    return testTSRegImmS16Instruction("addis", "ADDIS");
  }

  @TestFactory
  @Order(15)
  Stream<DynamicTest> subfic() throws IOException {
    return testTSRegImmS16Instruction("subfic", "SUBFIC");
  }

  /* not yet implemented
  @TestFactory
  @Order(16)
  Stream<DynamicTest> addg6s() throws IOException {
    return testTSSRegInstruction("addg6s", "ADDG6S");
  }
  */

  @TestFactory
  @Order(17)
  Stream<DynamicTest> addex() throws IOException {
    return testTSSRegInstructionExt("addex", "ADDEX", "0");
  }

  @TestFactory
  @Order(18)
  Stream<DynamicTest> mulli() throws IOException {
    return testTSRegImmS16Instruction("mulli", "MULLI");
  }

  @TestFactory
  @Order(19)
  Stream<DynamicTest> mullw() throws IOException {
    return testTSSRegInstruction_OR("mullw", "MULLW");
  }

  @TestFactory
  @Order(20)
  Stream<DynamicTest> mulhw() throws IOException {
    return testTSSRegInstruction_R("mulhw", "MULHW");
  }

  @TestFactory
  @Order(21)
  Stream<DynamicTest> mulhwu() throws IOException {
    return testTSSRegInstruction_R("mulhwu", "MULHWU");
  }

  @TestFactory
  @Order(22)
  Stream<DynamicTest> divw() throws IOException {
    return testTSSRegInstruction_OR("divw", "DIVW");
  }

  @TestFactory
  @Order(23)
  Stream<DynamicTest> divwu() throws IOException {
    return testTSSRegInstruction_OR("divwu", "DIVWU");
  }

  @TestFactory
  @Order(24)
  Stream<DynamicTest> divwe() throws IOException {
    return testTSSRegInstruction_OR("divwe", "DIVWE");
  }

  @TestFactory
  @Order(25)
  Stream<DynamicTest> divweu() throws IOException {
    return testTSSRegInstruction_OR("divweu", "DIVWEU");
  }

  @TestFactory
  @Order(26)
  Stream<DynamicTest> neg() throws IOException {
    return testTSRegInstruction_OR("neg", "NEG");
  }

  @TestFactory
  @Order(27)
  Stream<DynamicTest> modsw() throws IOException {
    return testTSSRegInstruction("modsw", "MODSW");
  }

  @TestFactory
  @Order(28)
  Stream<DynamicTest> moduw() throws IOException {
    return testTSSRegInstruction("moduw", "MODUW");
  }

  @TestFactory
  @Order(29)
  Stream<DynamicTest> and() throws IOException {
    return testTSSRegInstruction_R("and", "AND");
  }

  @TestFactory
  @Order(30)
  Stream<DynamicTest> andc() throws IOException {
    return testTSSRegInstruction_R("andc", "ANDC");
  }

  @TestFactory
  @Order(31)
  Stream<DynamicTest> eqv() throws IOException {
    return testTSSRegInstruction_R("eqv", "EQV");
  }

  @TestFactory
  @Order(32)
  Stream<DynamicTest> nand() throws IOException {
    return testTSSRegInstruction_R("nand", "NAND");
  }

  @TestFactory
  @Order(33)
  Stream<DynamicTest> nor() throws IOException {
    return testTSSRegInstruction_R("nor", "NOR");
  }

  @TestFactory
  @Order(34)
  Stream<DynamicTest> or() throws IOException {
    return testTSSRegInstruction_R("or", "OR");
  }

  @TestFactory
  @Order(35)
  Stream<DynamicTest> orc() throws IOException {
    return testTSSRegInstruction_R("orc", "ORC");
  }

  @TestFactory
  @Order(36)
  Stream<DynamicTest> xor() throws IOException {
    return testTSSRegInstruction_R("xor", "XOR");
  }

  @TestFactory
  @Order(37)
  Stream<DynamicTest> crand() throws IOException {
    return testTSSCRBitInstruction("crand", "CRAND");
  }

  @TestFactory
  @Order(38)
  Stream<DynamicTest> crandc() throws IOException {
    return testTSSCRBitInstruction("crandc", "CRANDC");
  }

  @TestFactory
  @Order(39)
  Stream<DynamicTest> creqv() throws IOException {
    return testTSSCRBitInstruction("creqv", "CREQV");
  }

  @TestFactory
  @Order(40)
  Stream<DynamicTest> crnand() throws IOException {
    return testTSSCRBitInstruction("crnand", "CRNAND");
  }

  @TestFactory
  @Order(41)
  Stream<DynamicTest> crnor() throws IOException {
    return testTSSCRBitInstruction("crnor", "CRNOR");
  }

  @TestFactory
  @Order(42)
  Stream<DynamicTest> cror() throws IOException {
    return testTSSCRBitInstruction("cror", "CROR");
  }

  @TestFactory
  @Order(43)
  Stream<DynamicTest> crorc() throws IOException {
    return testTSSCRBitInstruction("crorc", "CRORC");
  }

  @TestFactory
  @Order(44)
  Stream<DynamicTest> crxor() throws IOException {
    return testTSSCRBitInstruction("crxor", "CRXOR");
  }

  @TestFactory
  @Order(45)
  Stream<DynamicTest> andi_() throws IOException {
    return testTSRegImmU16Instruction("andi.", "ANDI.");
  }

  @TestFactory
  @Order(46)
  Stream<DynamicTest> andis_() throws IOException {
    return testTSRegImmU16Instruction("andis.", "ANDIS.");
  }

  @TestFactory
  @Order(47)
  Stream<DynamicTest> ori() throws IOException {
    return testTSRegImmU16Instruction("ori", "ORI");
  }

  @TestFactory
  @Order(48)
  Stream<DynamicTest> oris() throws IOException {
    return testTSRegImmU16Instruction("oris", "ORIS");
  }

  @TestFactory
  @Order(49)
  Stream<DynamicTest> xori() throws IOException {
    return testTSRegImmU16Instruction("xori", "XORI");
  }

  @TestFactory
  @Order(50)
  Stream<DynamicTest> xoris() throws IOException {
    return testTSRegImmU16Instruction("xoris", "XORIS");
  }

  @TestFactory
  @Order(51)
  Stream<DynamicTest> setb() throws IOException {
    return testTRegSCRFieldInstruction("setb", "SETB");
  }

  @TestFactory
  @Order(52)
  Stream<DynamicTest> setbc() throws IOException {
    return testTRegSCRBitInstruction("setbc", "SETBC");
  }

  @TestFactory
  @Order(53)
  Stream<DynamicTest> setbcr() throws IOException {
    return testTRegSCRBitInstruction("setbcr", "SETBCR");
  }

  @TestFactory
  @Order(54)
  Stream<DynamicTest> setnbc() throws IOException {
    return testTRegSCRBitInstruction("setnbc", "SETNBC");
  }

  @TestFactory
  @Order(55)
  Stream<DynamicTest> setnbcr() throws IOException {
    return testTRegSCRBitInstruction("setnbcr", "SETNBCR");
  }

  @TestFactory
  @Order(56)
  Stream<DynamicTest> rlwimi() throws IOException {
    return testMFormRotateInstruction_R("rlwimi", "RLWIMI");
  }

  @TestFactory
  @Order(57)
  Stream<DynamicTest> rlwinm() throws IOException {
    return testMFormRotateInstruction_R("rlwinm", "RLWINM");
  }

  @TestFactory
  @Order(58)
  Stream<DynamicTest> rlwnm() throws IOException {
    return testMFormRotateInstruction_R("rlwnm", "RLWNM");
  }

  @TestFactory
  @Order(59)
  Stream<DynamicTest> slw() throws IOException {
    return testXFormShiftInstruction_R("slw", "SLW");
  }

  @TestFactory
  @Order(60)
  Stream<DynamicTest> srw() throws IOException {
    return testXFormShiftInstruction_R("srw", "SRW");
  }

  @TestFactory
  @Order(61)
  Stream<DynamicTest> sraw() throws IOException {
    return testXFormShiftInstruction_R("sraw", "SRAW");
  }

  @TestFactory
  @Order(62)
  Stream<DynamicTest> srawi() throws IOException {
    return testXFormShiftInstruction_R("srawi", "SRAWI");
  }

  @TestFactory
  @Order(63)
  Stream<DynamicTest> cmp() throws IOException {
    return testTCRFieldModeSSRegInstruction("cmp", "CMP");
  }

  @TestFactory
  @Order(64)
  Stream<DynamicTest> cmpl() throws IOException {
    return testTCRFieldModeSSRegInstruction("cmpl", "CMPL");
  }

  @TestFactory
  @Order(65)
  Stream<DynamicTest> cmpi() throws IOException {
    return testDFormCompareInstruction("cmpi", "CMPI");
  }

  @TestFactory
  @Order(66)
  Stream<DynamicTest> cmpli() throws IOException {
    return testDFormCompareInstruction("cmpli", "CMPLI");
  }

  @TestFactory
  @Order(67)
  Stream<DynamicTest> cmpb() throws IOException {
    return testTSSRegInstruction("cmpb", "CMPB");
  }

  @TestFactory
  @Order(68)
  Stream<DynamicTest> cmpeqb() throws IOException {
    return testTCRFieldSSRegInstruction("cmpeqb", "CMPEQB");
  }

  @TestFactory
  @Order(69)
  Stream<DynamicTest> cmprb() throws IOException {
    return testTCRFieldModeSSRegInstruction("cmprb", "CMPRB");
  }

  @TestFactory
  @Order(70)
  Stream<DynamicTest> lbz() throws IOException {
    return testDFormLoadInstruction("lbz", "LBZ", false);
  }

  @TestFactory
  @Order(71)
  Stream<DynamicTest> lbzu() throws IOException {
    return testDFormLoadInstruction("lbzu", "LBZU", true);
  }

  @TestFactory
  @Order(72)
  Stream<DynamicTest> lha() throws IOException {
    return testDFormLoadInstruction("lha", "LHA", false);
  }

  @TestFactory
  @Order(73)
  Stream<DynamicTest> lhau() throws IOException {
    return testDFormLoadInstruction("lhau", "LHAU", true);
  }

  @TestFactory
  @Order(74)
  Stream<DynamicTest> lhz() throws IOException {
    return testDFormLoadInstruction("lhz", "LHZ", false);
  }

  @TestFactory
  @Order(75)
  Stream<DynamicTest> lhzu() throws IOException {
    return testDFormLoadInstruction("lhzu", "LHZU", true);
  }

  @TestFactory
  @Order(76)
  Stream<DynamicTest> lwz() throws IOException {
    return testDFormLoadInstruction("lwz", "LWZ", false);
  }

  @TestFactory
  @Order(77)
  Stream<DynamicTest> lwzu() throws IOException {
    return testDFormLoadInstruction("lwzu", "LWZU", true);
  }

  @TestFactory
  @Order(78)
  Stream<DynamicTest> lbzx() throws IOException {
    return testXFormLoadInstruction("lbzx", "LBZX", false);
  }

  @TestFactory
  @Order(79)
  Stream<DynamicTest> lbzux() throws IOException {
    return testXFormLoadInstruction("lbzux", "LBZUX", true);
  }

  @TestFactory
  @Order(80)
  Stream<DynamicTest> lhax() throws IOException {
    return testXFormLoadInstruction("lhax", "LHAX", false);
  }

  @TestFactory
  @Order(81)
  Stream<DynamicTest> lhaux() throws IOException {
    return testXFormLoadInstruction("lhaux", "LHAUX", true);
  }

  @TestFactory
  @Order(82)
  Stream<DynamicTest> lhzx() throws IOException {
    return testXFormLoadInstruction("lhzx", "LHZX", false);
  }

  @TestFactory
  @Order(83)
  Stream<DynamicTest> lhzux() throws IOException {
    return testXFormLoadInstruction("lhzux", "LHZUX", true);
  }

  @TestFactory
  @Order(84)
  Stream<DynamicTest> lwzx() throws IOException {
    return testXFormLoadInstruction("lwzx", "LWZX", false);
  }

  @TestFactory
  @Order(85)
  Stream<DynamicTest> lwzux() throws IOException {
    return testXFormLoadInstruction("lwzux", "LWZUX", true);
  }

  @TestFactory
  @Order(86)
  Stream<DynamicTest> lwbrx() throws IOException {
    return testXFormLoadInstruction("lwbrx", "LWBRX", false);
  }

  @TestFactory
  @Order(87)
  Stream<DynamicTest> stb() throws IOException {
    return testDFormStoreInstruction("stb", "STB", false);
  }

  @TestFactory
  @Order(88)
  Stream<DynamicTest> stbu() throws IOException {
    return testDFormStoreInstruction("stbu", "STBU", true);
  }

  @TestFactory
  @Order(89)
  Stream<DynamicTest> sth() throws IOException {
    return testDFormStoreInstruction("sth", "STH", false);
  }

  @TestFactory
  @Order(90)
  Stream<DynamicTest> sthu() throws IOException {
    return testDFormStoreInstruction("sthu", "STHU", true);
  }

  @TestFactory
  @Order(91)
  Stream<DynamicTest> stw() throws IOException {
    return testDFormStoreInstruction("stw", "STW", false);
  }

  @TestFactory
  @Order(92)
  Stream<DynamicTest> stwu() throws IOException {
    return testDFormStoreInstruction("stwu", "STWU", true);
  }

  @TestFactory
  @Order(93)
  Stream<DynamicTest> stbx() throws IOException {
    return testXFormStoreInstruction("stbx", "STBX", false);
  }

  @TestFactory
  @Order(94)
  Stream<DynamicTest> stbux() throws IOException {
    return testXFormStoreInstruction("stbux", "STBUX", true);
  }

  @TestFactory
  @Order(95)
  Stream<DynamicTest> sthx() throws IOException {
    return testXFormStoreInstruction("sthx", "STHX", false);
  }

  @TestFactory
  @Order(96)
  Stream<DynamicTest> sthux() throws IOException {
    return testXFormStoreInstruction("sthux", "STHUX", true);
  }

  @TestFactory
  @Order(97)
  Stream<DynamicTest> sthbrx() throws IOException {
    return testXFormStoreInstruction("sthbrx", "STHBRX", false);
  }

  @TestFactory
  @Order(98)
  Stream<DynamicTest> stwx() throws IOException {
    return testXFormStoreInstruction("stwx", "STWX", false);
  }

  @TestFactory
  @Order(99)
  Stream<DynamicTest> stwux() throws IOException {
    return testXFormStoreInstruction("stwux", "STWUX", true);
  }

  @TestFactory
  @Order(100)
  Stream<DynamicTest> stwbrx() throws IOException {
    return testXFormStoreInstruction("stwbrx", "STWBRX", false);
  }

  @TestFactory
  @Order(101)
  Stream<DynamicTest> mtocrf() throws IOException {
    return testOCRMask8SRegInstruction("mtocrf", "MTOCRF");
  }

  @TestFactory
  @Order(102)
  Stream<DynamicTest> mfocrf() throws IOException {
    return testTRegOCRMask8Instruction("mfocrf", "MFOCRF");
  }

  @TestFactory
  @Order(103)
  Stream<DynamicTest> mfcr() throws IOException {
    return testTRegCRSInstruction("mfcr", "MFCR");
  }

  @TestFactory
  @Order(104)
  Stream<DynamicTest> mtcrf() throws IOException {
    return testCRMask8SRegInstruction("mtcrf", "MTCRF");
  }

  @TestFactory
  @Order(105)
  Stream<DynamicTest> mcrf() throws IOException {
    return testTSCRFieldInstruction("mcrf", "MCRF");
  }

  @TestFactory
  @Order(106)
  Stream<DynamicTest> mcrxrx() throws IOException {
    return testXFormMoveXERCRFieldInstruction("mcrxrx", "MCRXRX");
  }

  @TestFactory
  @Order(107)
  Stream<DynamicTest> brh() throws IOException {
    return testTSRegInstruction("brh", "BRH");
  }

  @TestFactory
  @Order(108)
  Stream<DynamicTest> brw() throws IOException {
    return testTSRegInstruction("brw", "BRW");
  }

  @TestFactory
  @Order(109)
  Stream<DynamicTest> cbcdtd() throws IOException {
    return testTSRegInstruction("cbcdtd", "CBCDTD");
  }

  @TestFactory
  @Order(110)
  Stream<DynamicTest> cdtbcd() throws IOException {
    return testTSRegInstruction("cdtbcd", "CDTBCD");
  }

  @TestFactory
  @Order(111)
  Stream<DynamicTest> popcntb() throws IOException {
    return testTSRegInstruction("popcntb", "POPCNTB");
  }

  @TestFactory
  @Order(112)
  Stream<DynamicTest> popcntw() throws IOException {
    return testTSRegInstruction("popcntw", "POPCNTW");
  }

  @TestFactory
  @Order(113)
  Stream<DynamicTest> prtyw() throws IOException {
    return testTSRegInstruction("prtyw", "PRTYW");
  }

  @TestFactory
  @Order(114)
  Stream<DynamicTest> extsb() throws IOException {
    return testTSRegInstruction_R("extsb", "EXTSB");
  }

  @TestFactory
  @Order(115)
  Stream<DynamicTest> extsh() throws IOException {
    return testTSRegInstruction_R("extsh", "EXTSH");
  }

  @TestFactory
  @Order(116)
  Stream<DynamicTest> cntlzw() throws IOException {
    return testTSRegInstruction_R("cntlzw", "CNTLZW");
  }

  @TestFactory
  @Order(117)
  Stream<DynamicTest> cnttzw() throws IOException {
    return testTSRegInstruction_R("cnttzw", "CNTTZW");
  }

  private Stream<DynamicTest> testTSSRegInstruction_OR(String instruction, String name)
      throws IOException {
    var s1 = testTSSRegInstruction(instruction, name);
    var s2 = testTSSRegInstruction(instruction + ".", name + ".");
    var s3 = testTSSRegInstruction(instruction + "o", name + "O");
    var s4 = testTSSRegInstruction(instruction + "o.", name + "O.");
    return Stream.concat(Stream.concat(s1, s2), Stream.concat(s3, s4));
  }

  private Stream<DynamicTest> testTSSRegInstruction_R(String instruction, String name)
      throws IOException {
    var s1 = testTSSRegInstruction(instruction, name);
    var s2 = testTSSRegInstruction(instruction + ".", name + ".");
    return Stream.concat(s1, s2);
  }

  private Stream<DynamicTest> testTSRegInstruction_R(String instruction, String name)
      throws IOException {
    var s1 = testTSRegInstruction(instruction, name);
    var s2 = testTSRegInstruction(instruction + ".", name + ".");
    return Stream.concat(s1, s2);
  }

  private Stream<DynamicTest> testTSRegInstruction_OR(String instruction, String name)
      throws IOException {
    var s1 = testTSRegInstruction(instruction, name);
    var s2 = testTSRegInstruction(instruction + ".", name + ".");
    var s3 = testTSRegInstruction(instruction + "o", name + "O");
    var s4 = testTSRegInstruction(instruction + "o.", name + "O.");
    return Stream.concat(Stream.concat(s1, s2), Stream.concat(s3, s4));
  }

  private Stream<DynamicTest> testTSRegImmS16Instruction_R(String instruction, String name)
      throws IOException {
    var s1 = testTSRegImmS16Instruction(instruction, name);
    var s2 = testTSRegImmS16Instruction(instruction + ".", name + ".");
    return Stream.concat(s1, s2);
  }

  private Stream<DynamicTest> testMFormRotateInstruction_R(String instruction, String name)
      throws IOException {
    var s1 = testMFormRotateInstruction(instruction, name);
    var s2 = testMFormRotateInstruction(instruction + ".", name + ".");
    return Stream.concat(s1, s2);
  }

  private Stream<DynamicTest> testXFormShiftInstruction_R(String instruction, String name)
      throws IOException {
    var s1 = testXFormShiftInstruction(instruction, name);
    var s2 = testXFormShiftInstruction(instruction + ".", name + ".");
    return Stream.concat(s1, s2);
  }

  private Stream<DynamicTest> testTSSRegInstruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc1 = b.anyReg().sample();
      var regSrc2 = b.anyReg().sample();
      b.fillReg(regSrc1);
      b.fillReg(regSrc2);
      b.add("%s %s, %s, %s", instruction, b.anyReg().sample(), regSrc1, regSrc2);
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testTSSRegInstructionExt(String instruction, String name, String ext)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc1 = b.anyReg().sample();
      var regSrc2 = b.anyReg().sample();
      b.fillReg(regSrc1);
      b.fillReg(regSrc2);
      b.add("%s %s, %s, %s, %s", instruction, b.anyReg().sample(), regSrc1, regSrc2, ext);
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testTCRFieldSSRegInstruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc1 = b.anyReg().sample();
      var regSrc2 = b.anyReg().sample();
      b.fillReg(regSrc1);
      b.fillReg(regSrc2);
      b.add("%s %s, %s, %s", instruction, b.anyCRField().sample(), regSrc1, regSrc2);
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testTSCRFieldInstruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      b.fillCR();
      b.add("%s %s, %s", instruction, b.anyCRField().sample(), b.anyCRField().sample());
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testTSSCRBitInstruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      b.fillCR();
      b.add("%s %s, %s, %s", instruction, b.anyCRBit().sample(), b.anyCRBit().sample(),
          b.anyCRBit().sample());
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testTSRegImmS16Instruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc = b.anyReg().sample();
      b.fillReg(regSrc);
      b.add("%s %s, %s, %s", instruction, b.anyReg().sample(), regSrc, b.anyImmS(16));
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testTSRegImmU16Instruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc = b.anyReg().sample();
      b.fillReg(regSrc);
      b.add("%s %s, %s, %s", instruction, b.anyReg().sample(), regSrc, b.anyImmU(16));
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testTSRegInstruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc = b.anyReg().sample();
      b.fillReg(regSrc);
      b.add("%s %s, %s", instruction, b.anyReg().sample(), regSrc);
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testTRegSCRFieldInstruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      b.fillCR();
      b.add("%s %s, %s", instruction, b.anyReg().sample(), b.anyCRField().sample());
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testTRegSCRBitInstruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      b.fillCR();
      b.add("%s %s, %s", instruction, b.anyReg().sample(), b.anyCRBit().sample());
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testTRegOCRMask8Instruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      b.fillCR();
      b.add("%s %s, %s", instruction, b.anyReg().sample(), b.anySelectImmU(8));
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testCRMask8SRegInstruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc = b.anyReg().sample();
      b.fillCR();
      b.fillReg(regSrc);
      b.add("%s %s, %s", instruction, b.anyImmU(8), regSrc);
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testOCRMask8SRegInstruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc = b.anyReg().sample();
      b.fillCR();
      b.fillReg(regSrc);
      b.add("%s %s, %s", instruction, b.anySelectImmU(8), regSrc);
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testTRegImmS16Instruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      b.add("%s %s, %s", instruction, b.anyReg().sample(), b.anyImmS(16));
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testTRegCRSInstruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      b.fillCR();
      b.add("%s %s", instruction, b.anyReg().sample());
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testTCRFieldModeSSRegInstruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc1 = b.anyReg().sample();
      var regSrc2 = b.anyReg().sample();
      b.fillReg(regSrc1);
      b.fillReg(regSrc2);
      b.add("%s %s, %s, %s, %s", instruction, b.anyCRField().sample(), b.anyImmU(1), regSrc1,
          regSrc2);
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testDFormCompareInstruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc = b.anyReg().sample();
      b.fillReg(regSrc);
      b.add("%s %s, %s, %s, %s", instruction, b.anyCRField().sample(), b.anyImmU(1), regSrc,
          b.anyImmS(16));
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testMFormRotateInstruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc = b.anyReg().sample();
      var regSrcOrImm5 = b.anyReg().sample();
      b.fillReg(regSrc);
      b.fillReg(regSrcOrImm5);
      b.add("%s %s, %s, %s, %s, %s", instruction, b.anyReg().sample(), regSrc, regSrcOrImm5,
          b.anyImmU(5), b.anyImmU(5));
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testXFormShiftInstruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrcOrImm5 = b.anyReg().sample();
      var regSrc = b.anyReg().sample();
      b.fillReg(regSrcOrImm5);
      b.fillReg(regSrc);
      b.add("%s %s, %s, %s", instruction, b.anyReg().sample(), regSrcOrImm5, regSrc);
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testXFormMoveXERCRFieldInstruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc1 = b.anyReg().sample();
      var regSrc2 = b.anyReg().sample();
      b.fillCR();
      b.fillReg(regSrc1);
      b.fillReg(regSrc2);
      b.add("addco %s, %s, %s", b.anyReg().sample(), regSrc1, regSrc2); // Sets XER
      b.add("%s %s", instruction, b.anyCRField().sample());
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testDFormLoadInstruction(String instruction, String name,
                                                       boolean update)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc = update ? b.anyRegExceptZero().sample() : b.anyReg().sample();
      var regDest = update ? b.anyReg().filter(r -> !Objects.equals(r, regSrc)).sample() :
          b.anyReg().sample();
      BigInteger val1 = b.anyImmU(15);
      BigInteger val2 = b.anyImmUFrom(15, BigInteger.valueOf(BASE_ADDRESS_LOAD_STORE));
      b.fillMem(Objects.equals(regSrc, "0") ? val2 : val1.add(val2));
      b.fillReg(regSrc, val1);
      b.add("%s %s, %s(%s)", instruction, regDest, val2, regSrc);
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testXFormLoadInstruction(String instruction, String name,
                                                       boolean update)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc1 = update ? b.anyRegExceptZero().sample() : b.anyReg().sample();
      var regSrc2 = b.anyReg().sample();
      var regDest = update ? b.anyReg().filter(r -> !Objects.equals(r, regSrc1)).sample() :
          b.anyReg().filter(r -> !Objects.equals(r, regSrc1)).sample();
      BigInteger val1 = b.anyImmU(15);
      BigInteger val2 = b.anyImmUFrom(15, BigInteger.valueOf(BASE_ADDRESS_LOAD_STORE));
      b.fillMem(Objects.equals(regSrc1, "0") ? val2 : val1.add(val2));
      b.fillReg(regSrc1, val1);
      b.fillReg(regSrc2, val2);
      b.add("%s %s, %s, %s", instruction, regDest, regSrc1, regSrc2);
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testDFormStoreInstruction(String instruction, String name,
                                                        boolean update)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc1 = b.anyReg().sample();
      var regSrc2 = update ? b.anyRegExceptZero().sample() : b.anyReg().sample();
      b.fillReg(regSrc1);
      b.fillReg(regSrc2, b.anyImmU(15));
      b.add("%s %s, %s(%s)", instruction, regSrc1, b.anyImmUFrom(15, BigInteger.valueOf(BASE_ADDRESS_LOAD_STORE)), regSrc2);
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testXFormStoreInstruction(String instruction, String name,
                                                        boolean update)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc1 = b.anyReg().sample();
      var regSrc2 = update ? b.anyRegExceptZero().sample() : b.anyReg().sample();
      var regSrc3 = b.anyReg().sample();
      b.fillReg(regSrc1);
      b.fillReg(regSrc2, b.anyImmUFrom(15, BigInteger.valueOf(BASE_ADDRESS_LOAD_STORE)));
      b.fillReg(regSrc3, b.anyImmU(15));
      b.add("%s %s, %s, %s", instruction, regSrc1, regSrc2, regSrc3);
      return b.toTestCase();
    });
  }

}
