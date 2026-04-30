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

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestMethodOrder;
import vadl.iss.CosimTestUtils;

/* Tests ppc64.vadl instructions against the QEMU ppc64 simulator.
 * Some instructions are defined in the VADL specification but are not covered by these tests:
 *   mfmsr, mtmsr, mfspr, mtspr and all branch instructions.
 * Load/Store instructions are tested with a reduced address space:
 *   0x0000'0000'0000'1000 - 0x0000'0000'0000'FFFF
 */

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CosimPpc64InstrTest extends AbstractCosimPpc64InstrTest {

  private static final long BASE_ADDRESS_LOAD_STORE = 0x1000L;
  private static final Object INIT_LOCK = new Object();

  private static List<CosimTestUtils.TestCase> testCases;
  private static Map<String, TestResult> failures;

  // We cannot use @BeforeAll here because it runs before AbstractTest.beforeEach initializes
  // the frontend required by generateIssSimulator().
  @Test
  @Order(1)
  void setupInstructionTests() throws IOException {
    ensureInstructionTestsInitialized();
  }

  @TestFactory
  @Order(2)
  Stream<DynamicTest> buildInstructionTests() throws IOException {
    ensureInstructionTestsInitialized();
    return testCases.stream()
        .map(testCase -> DynamicTest.dynamicTest(
            testCase.id(),
            () -> {
              var failure = failures.get(testCase.id());
              if (failure != null && !failure.passed()) {
                fail(failure.failureMessage());
              }
            }));
  }

  private void ensureInstructionTestsInitialized() throws IOException {
    synchronized (INIT_LOCK) {
      if (testCases != null && failures != null) {
        return;
      }
      testCases = buildInstructionTestCases();
      failures = runQemuInstrTestsAndCollectFailures(generateIssSimulator(getVadlSpec()), testCases);
    }
  }

  private List<CosimTestUtils.TestCase> buildInstructionTestCases() throws IOException {
    return concatInstructionTests(List.of(
        () -> testTRegImmS16Instruction("addpcis", "ADDPCIS"),
        () -> testTSSRegInstruction_OR("add", "ADD"),
        () -> testTSSRegInstruction_OR("addc", "ADDC"),
        () -> testTSSRegInstruction_OR("adde", "ADDE"),
        () -> testTSSRegInstruction_OR("subf", "SUBF"),
        () -> testTSSRegInstruction_OR("subfc", "SUBFC"),
        () -> testTSSRegInstruction_OR("subfe", "SUBFE"),
        () -> testTSRegInstruction_OR("addme", "ADDME"),
        () -> testTSRegInstruction_OR("addze", "ADDZE"),
        () -> testTSRegInstruction_OR("subfme", "SUBFME"),
        () -> testTSRegInstruction_OR("subfze", "SUBFZE"),
        () -> testTSRegImmS16Instruction("addi", "ADDI"),
        () -> testTSRegImmS16Instruction_R("addic", "ADDIC"),
        () -> testTSRegImmS16Instruction("addis", "ADDIS"),
        () -> testTSRegImmS16Instruction("subfic", "SUBFIC"),
        () -> testTSSRegInstruction("addg6s", "ADDG6S"),
        () -> testTSSRegInstructionExt("addex", "ADDEX", "0"),
        () -> testTSRegImmS16Instruction("mulli", "MULLI"),
        () -> testTSSRegInstruction_OR("mullw", "MULLW"),
        () -> testTSSRegInstruction_R("mulhw", "MULHW"),
        () -> testTSSRegInstruction_R("mulhwu", "MULHWU"),
        () -> testTSSRegInstruction_OR("divw", "DIVW"),
        () -> testTSSRegInstruction_OR("divwu", "DIVWU"),
        () -> testTSSRegInstruction_OR("divwe", "DIVWE"),
        () -> testTSSRegInstruction_OR("divweu", "DIVWEU"),
        () -> testTSRegInstruction_OR("neg", "NEG"),
        () -> testTSSRegInstruction("modsw", "MODSW"),
        () -> testTSSRegInstruction("moduw", "MODUW"),
        () -> testTSSRegInstruction_R("and", "AND"),
        () -> testTSSRegInstruction_R("andc", "ANDC"),
        () -> testTSSRegInstruction_R("eqv", "EQV"),
        () -> testTSSRegInstruction_R("nand", "NAND"),
        () -> testTSSRegInstruction_R("nor", "NOR"),
        () -> testTSSRegInstruction_R("or", "OR"),
        () -> testTSSRegInstruction_R("orc", "ORC"),
        () -> testTSSRegInstruction_R("xor", "XOR"),
        () -> testTSSCRBitInstruction("crand", "CRAND"),
        () -> testTSSCRBitInstruction("crandc", "CRANDC"),
        () -> testTSSCRBitInstruction("creqv", "CREQV"),
        () -> testTSSCRBitInstruction("crnand", "CRNAND"),
        () -> testTSSCRBitInstruction("crnor", "CRNOR"),
        () -> testTSSCRBitInstruction("cror", "CROR"),
        () -> testTSSCRBitInstruction("crorc", "CRORC"),
        () -> testTSSCRBitInstruction("crxor", "CRXOR"),
        () -> testTSRegImmU16Instruction("andi.", "ANDI."),
        () -> testTSRegImmU16Instruction("andis.", "ANDIS."),
        () -> testTSRegImmU16Instruction("ori", "ORI"),
        () -> testTSRegImmU16Instruction("oris", "ORIS"),
        () -> testTSRegImmU16Instruction("xori", "XORI"),
        () -> testTSRegImmU16Instruction("xoris", "XORIS"),
        () -> testTRegSCRFieldInstruction("setb", "SETB"),
        () -> testTRegSCRBitInstruction("setbc", "SETBC"),
        () -> testTRegSCRBitInstruction("setbcr", "SETBCR"),
        () -> testTRegSCRBitInstruction("setnbc", "SETNBC"),
        () -> testTRegSCRBitInstruction("setnbcr", "SETNBCR"),
        () -> testMFormRotateInstruction_R("rlwimi", "RLWIMI"),
        () -> testMFormRotateInstruction_R("rlwinm", "RLWINM"),
        () -> testMFormRotateInstruction_R("rlwnm", "RLWNM"),
        () -> testXFormShiftInstruction_R("slw", "SLW"),
        () -> testXFormShiftInstruction_R("srw", "SRW"),
        () -> testXFormShiftInstruction_R("sraw", "SRAW"),
        () -> testXFormShiftInstruction_R("srawi", "SRAWI"),
        () -> testTCRFieldModeSSRegInstruction("cmp", "CMP"),
        () -> testTCRFieldModeSSRegInstruction("cmpl", "CMPL"),
        () -> testDFormCompareInstruction("cmpi", "CMPI"),
        () -> testDFormCompareInstruction("cmpli", "CMPLI"),
        () -> testTSSRegInstruction("cmpb", "CMPB"),
        () -> testTCRFieldSSRegInstruction("cmpeqb", "CMPEQB"),
        () -> testTCRFieldModeSSRegInstruction("cmprb", "CMPRB"),
        () -> testDFormLoadInstruction("lbz", "LBZ", false),
        () -> testDFormLoadInstruction("lbzu", "LBZU", true),
        () -> testDFormLoadInstruction("lha", "LHA", false),
        () -> testDFormLoadInstruction("lhau", "LHAU", true),
        () -> testDFormLoadInstruction("lhz", "LHZ", false),
        () -> testDFormLoadInstruction("lhzu", "LHZU", true),
        () -> testDFormLoadInstruction("lwz", "LWZ", false),
        () -> testDFormLoadInstruction("lwzu", "LWZU", true),
        () -> testXFormLoadInstruction("lbzx", "LBZX", false),
        () -> testXFormLoadInstruction("lbzux", "LBZUX", true),
        () -> testXFormLoadInstruction("lhax", "LHAX", false),
        () -> testXFormLoadInstruction("lhaux", "LHAUX", true),
        () -> testXFormLoadInstruction("lhzx", "LHZX", false),
        () -> testXFormLoadInstruction("lhzux", "LHZUX", true),
        () -> testXFormLoadInstruction("lwzx", "LWZX", false),
        () -> testXFormLoadInstruction("lwzux", "LWZUX", true),
        () -> testXFormLoadInstruction("lwbrx", "LWBRX", false),
        () -> testDFormStoreInstruction("stb", "STB", false),
        () -> testDFormStoreInstruction("stbu", "STBU", true),
        () -> testDFormStoreInstruction("sth", "STH", false),
        () -> testDFormStoreInstruction("sthu", "STHU", true),
        () -> testDFormStoreInstruction("stw", "STW", false),
        () -> testDFormStoreInstruction("stwu", "STWU", true),
        () -> testXFormStoreInstruction("stbx", "STBX", false),
        () -> testXFormStoreInstruction("stbux", "STBUX", true),
        () -> testXFormStoreInstruction("sthx", "STHX", false),
        () -> testXFormStoreInstruction("sthux", "STHUX", true),
        () -> testXFormStoreInstruction("sthbrx", "STHBRX", false),
        () -> testXFormStoreInstruction("stwx", "STWX", false),
        () -> testXFormStoreInstruction("stwux", "STWUX", true),
        () -> testXFormStoreInstruction("stwbrx", "STWBRX", false),
        () -> testOCRMask8SRegInstruction("mtocrf", "MTOCRF"),
        () -> testTRegOCRMask8Instruction("mfocrf", "MFOCRF"),
        () -> testTRegCRSInstruction("mfcr", "MFCR"),
        () -> testCRMask8SRegInstruction("mtcrf", "MTCRF"),
        () -> testTSCRFieldInstruction("mcrf", "MCRF"),
        () -> testXFormMoveXERCRFieldInstruction("mcrxrx", "MCRXRX"),
        () -> testTSRegInstruction("brh", "BRH"),
        () -> testTSRegInstruction("brw", "BRW"),
        () -> testTSRegInstruction("cbcdtd", "CBCDTD"),
        () -> testTSRegInstruction("cdtbcd", "CDTBCD"),
        () -> testTSRegInstruction("popcntb", "POPCNTB"),
        () -> testTSRegInstruction("popcntw", "POPCNTW"),
        () -> testTSRegInstruction("prtyw", "PRTYW"),
        () -> testTSRegInstruction_R("extsb", "EXTSB"),
        () -> testTSRegInstruction_R("extsh", "EXTSH"),
        () -> testTSRegInstruction_R("cntlzw", "CNTLZW"),
        () -> testTSRegInstruction_R("cnttzw", "CNTTZW")));
  }

  private List<CosimTestUtils.TestCase> concatInstructionTests(
      List<InstructionTestFactory> testFactories)
      throws IOException {
    List<CosimTestUtils.TestCase> tests = new ArrayList<>();
    for (var testFactory : testFactories) {
      tests.addAll(testFactory.create());
    }
    return tests;
  }

  @FunctionalInterface
  private interface InstructionTestFactory {
    List<CosimTestUtils.TestCase> create() throws IOException;
  }

  private List<CosimTestUtils.TestCase> testTSSRegInstruction_OR(String instruction, String name)
      throws IOException {
    return List.of(
            testTSSRegInstruction(instruction, name),
            testTSSRegInstruction(instruction + ".", name + "."),
            testTSSRegInstruction(instruction + "o", name + "O"),
            testTSSRegInstruction(instruction + "o.", name + "O."))
        .stream()
        .flatMap(List::stream)
        .toList();
  }

  private List<CosimTestUtils.TestCase> testTSSRegInstruction_R(String instruction, String name)
      throws IOException {
    return List.of(
            testTSSRegInstruction(instruction, name),
            testTSSRegInstruction(instruction + ".", name + "."))
        .stream()
        .flatMap(List::stream)
        .toList();
  }

  private List<CosimTestUtils.TestCase> testTSRegInstruction_R(String instruction, String name)
      throws IOException {
    return List.of(
            testTSRegInstruction(instruction, name),
            testTSRegInstruction(instruction + ".", name + "."))
        .stream()
        .flatMap(List::stream)
        .toList();
  }

  private List<CosimTestUtils.TestCase> testTSRegInstruction_OR(String instruction, String name)
      throws IOException {
    return List.of(
            testTSRegInstruction(instruction, name),
            testTSRegInstruction(instruction + ".", name + "."),
            testTSRegInstruction(instruction + "o", name + "O"),
            testTSRegInstruction(instruction + "o.", name + "O."))
        .stream()
        .flatMap(List::stream)
        .toList();
  }

  private List<CosimTestUtils.TestCase> testTSRegImmS16Instruction_R(String instruction,
                                                                     String name)
      throws IOException {
    return List.of(
            testTSRegImmS16Instruction(instruction, name),
            testTSRegImmS16Instruction(instruction + ".", name + "."))
        .stream()
        .flatMap(List::stream)
        .toList();
  }

  private List<CosimTestUtils.TestCase> testMFormRotateInstruction_R(String instruction,
                                                                     String name)
      throws IOException {
    return List.of(
            testMFormRotateInstruction(instruction, name),
            testMFormRotateInstruction(instruction + ".", name + "."))
        .stream()
        .flatMap(List::stream)
        .toList();
  }

  private List<CosimTestUtils.TestCase> testXFormShiftInstruction_R(String instruction, String name)
      throws IOException {
    return List.of(
            testXFormShiftInstruction(instruction, name),
            testXFormShiftInstruction(instruction + ".", name + "."))
        .stream()
        .flatMap(List::stream)
        .toList();
  }

  private List<CosimTestUtils.TestCase> testTSSRegInstruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc1 = b.anyReg().sample();
      var regSrc2 = b.anyReg().sample();
      b.fillReg(regSrc1);
      b.fillReg(regSrc2);
      b.add("%s %s, %s, %s", instruction, b.anyReg().sample(), regSrc1, regSrc2);
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testTSSRegInstructionExt(String instruction, String name,
                                                                 String ext)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc1 = b.anyReg().sample();
      var regSrc2 = b.anyReg().sample();
      b.fillReg(regSrc1);
      b.fillReg(regSrc2);
      b.add("%s %s, %s, %s, %s", instruction, b.anyReg().sample(), regSrc1, regSrc2, ext);
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testTCRFieldSSRegInstruction(String instruction,
                                                                     String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc1 = b.anyReg().sample();
      var regSrc2 = b.anyReg().sample();
      b.fillReg(regSrc1);
      b.fillReg(regSrc2);
      b.add("%s %s, %s, %s", instruction, b.anyCRField().sample(), regSrc1, regSrc2);
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testTSCRFieldInstruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      b.fillCR();
      b.add("%s %s, %s", instruction, b.anyCRField().sample(), b.anyCRField().sample());
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testTSSCRBitInstruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      b.fillCR();
      b.add("%s %s, %s, %s", instruction, b.anyCRBit().sample(), b.anyCRBit().sample(),
          b.anyCRBit().sample());
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testTSRegImmS16Instruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc = b.anyReg().sample();
      b.fillReg(regSrc);
      b.add("%s %s, %s, %s", instruction, b.anyReg().sample(), regSrc, b.getImmS(16));
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testTSRegImmU16Instruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc = b.anyReg().sample();
      b.fillReg(regSrc);
      b.add("%s %s, %s, %s", instruction, b.anyReg().sample(), regSrc, b.getImmU(16));
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testTSRegInstruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc = b.anyReg().sample();
      b.fillReg(regSrc);
      b.add("%s %s, %s", instruction, b.anyReg().sample(), regSrc);
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testTRegSCRFieldInstruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      b.fillCR();
      b.add("%s %s, %s", instruction, b.anyReg().sample(), b.anyCRField().sample());
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testTRegSCRBitInstruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      b.fillCR();
      b.add("%s %s, %s", instruction, b.anyReg().sample(), b.anyCRBit().sample());
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testTRegOCRMask8Instruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      b.fillCR();
      b.add("%s %s, %s", instruction, b.anyReg().sample(), b.getSelectImmU(8));
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testCRMask8SRegInstruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc = b.anyReg().sample();
      b.fillCR();
      b.fillReg(regSrc);
      b.add("%s %s, %s", instruction, b.getImmU(8), regSrc);
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testOCRMask8SRegInstruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc = b.anyReg().sample();
      b.fillCR();
      b.fillReg(regSrc);
      b.add("%s %s, %s", instruction, b.getSelectImmU(8), regSrc);
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testTRegImmS16Instruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      b.add("%s %s, %s", instruction, b.anyReg().sample(), b.getImmS(16));
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testTRegCRSInstruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      b.fillCR();
      b.add("%s %s", instruction, b.anyReg().sample());
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testTCRFieldModeSSRegInstruction(String instruction,
                                                                         String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc1 = b.anyReg().sample();
      var regSrc2 = b.anyReg().sample();
      b.fillReg(regSrc1);
      b.fillReg(regSrc2);
      b.add("%s %s, %s, %s, %s", instruction, b.anyCRField().sample(), b.getImmU(1), regSrc1,
          regSrc2);
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testDFormCompareInstruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc = b.anyReg().sample();
      b.fillReg(regSrc);
      b.add("%s %s, %s, %s, %s", instruction, b.anyCRField().sample(), b.getImmU(1), regSrc,
          b.getImmS(16));
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testMFormRotateInstruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc = b.anyReg().sample();
      var regSrcOrImm5 = b.anyReg().sample();
      b.fillReg(regSrc);
      b.fillReg(regSrcOrImm5);
      b.add("%s %s, %s, %s, %s, %s", instruction, b.anyReg().sample(), regSrc, regSrcOrImm5,
          b.getImmU(5), b.getImmU(5));
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testXFormShiftInstruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrcOrImm5 = b.anyReg().sample();
      var regSrc = b.anyReg().sample();
      b.fillReg(regSrcOrImm5);
      b.fillReg(regSrc);
      b.add("%s %s, %s, %s", instruction, b.anyReg().sample(), regSrcOrImm5, regSrc);
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testXFormMoveXERCRFieldInstruction(String instruction,
                                                                           String name)
      throws IOException {
    return buildTests3264With(id -> {
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

  private List<CosimTestUtils.TestCase> testDFormLoadInstruction(String instruction, String name,
                                                                 boolean update)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc = update ? b.anyRegExceptZero().sample() : b.anyReg().sample();
      var regDest = update ? b.anyReg().filter(r -> !Objects.equals(r, regSrc)).sample() :
          b.anyReg().sample();
      BigInteger val1 = b.getImmU(15);
      BigInteger val2 = b.getImmUFrom(15, BigInteger.valueOf(BASE_ADDRESS_LOAD_STORE));
      b.fillMem(Objects.equals(regSrc, "0") ? val2 : val1.add(val2));
      b.fillReg(regSrc, val1);
      b.add("%s %s, %s(%s)", instruction, regDest, val2, regSrc);
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testXFormLoadInstruction(String instruction, String name,
                                                                 boolean update)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc1 = update ? b.anyRegExceptZero().sample() : b.anyReg().sample();
      var regSrc2 = b.anyReg().filter(r -> !Objects.equals(r, regSrc1)).sample();
      var regDest = update ? b.anyReg().filter(r -> !Objects.equals(r, regSrc1)).sample() :
          b.anyReg().sample();
      BigInteger val1 = b.getImmU(15);
      BigInteger val2 = b.getImmUFrom(15, BigInteger.valueOf(BASE_ADDRESS_LOAD_STORE));
      b.fillMem(Objects.equals(regSrc1, "0") ? val2 : val1.add(val2));
      b.fillReg(regSrc1, val1);
      b.fillReg(regSrc2, val2);
      b.add("%s %s, %s, %s", instruction, regDest, regSrc1, regSrc2);
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testDFormStoreInstruction(String instruction, String name,
                                                                  boolean update)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc1 = b.anyReg().sample();
      var regSrc2 = update ? b.anyRegExceptZero().sample() : b.anyReg().sample();
      b.fillReg(regSrc1);
      b.fillReg(regSrc2, b.getImmU(15));
      b.add("%s %s, %s(%s)", instruction, regSrc1,
          b.getImmUFrom(15, BigInteger.valueOf(BASE_ADDRESS_LOAD_STORE)), regSrc2);
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testXFormStoreInstruction(String instruction, String name,
                                                                  boolean update)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      var regSrc1 = b.anyReg().sample();
      var regSrc2 = update ? b.anyRegExceptZero().sample() : b.anyReg().sample();
      var regSrc3 = b.anyReg().sample();
      b.fillReg(regSrc1);
      b.fillReg(regSrc2, b.getImmUFrom(15, BigInteger.valueOf(BASE_ADDRESS_LOAD_STORE)));
      b.fillReg(regSrc3, b.getImmU(15));
      b.add("%s %s, %s, %s", instruction, regSrc1, regSrc2, regSrc3);
      return b.toTestCase();
    });
  }

}
