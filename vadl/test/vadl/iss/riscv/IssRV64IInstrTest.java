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

import static vadl.TestUtils.arbitrarySignedInt;
import static vadl.TestUtils.arbitraryUnsignedInt;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
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
public class IssRV64IInstrTest extends AbstractIssRiscv64InstrTest {

  private static final String VADL_SPEC = "sys/risc-v/rv64v.vadl";
  private static final int TESTS_PER_INSTRUCTION = 50;
  // The test linker places .text.init at 0x80000000 and aligns .tohost to the next 4 KiB page.
  // Keep randomized load/store scratch addresses above that reserved startup/HTIF region.
  private static final BigInteger TEST_DATA_MIN_ADDR = BigInteger.valueOf(0x80002000L);
  private static final BigInteger TEST_DATA_MAX_ADDR = BigInteger.valueOf(0x800F0000L);

  @Override
  public int getTestPerInstruction() {
    return TESTS_PER_INSTRUCTION;
  }

  @Override
  public String getVadlSpec() {
    return VADL_SPEC;
  }

  public AsmTestBuilder getBuilder(String testNamePrefix, int id) {
    return new RV64IMVTestBuilder(testNamePrefix + "_" + id);
  }

  // We cannot use @BeforeAll here because it runs before AbstractTest.beforeEach initializes
  // the frontend required by generateIssSimulator().
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

  private List<IssTestUtils.TestCase> buildInstructionTestCases() {
    var tests = new java.util.ArrayList<IssTestUtils.TestCase>();
    tests.addAll(testBinaryRegRegInstruction("add", "ADD"));
    tests.addAll(testBinaryRegRegInstruction("sub", "SUB"));
    tests.addAll(testBinaryRegRegInstruction("and", "AND"));
    tests.addAll(testBinaryRegRegInstruction("or", "OR"));
    tests.addAll(testBinaryRegRegInstruction("xor", "XOR"));
    tests.addAll(testBinaryRegRegInstruction("slt", "SLT"));
    tests.addAll(testBinaryRegRegInstruction("sltu", "SLTU"));
    tests.addAll(testBinaryRegImmInstruction("addi", "ADDI"));
    tests.addAll(testBinaryRegImmInstruction("andi", "ANDI"));
    tests.addAll(testBinaryRegImmInstruction("ori", "ORI"));
    tests.addAll(testBinaryRegImmInstruction("xori", "XORI"));
    tests.addAll(testBinaryRegImmInstruction("slti", "SLTI"));
    tests.addAll(testBinaryRegImmInstruction("sltiu", "SLTIU"));
    tests.addAll(testShiftImmInstruction("slli", "SLLI"));
    tests.addAll(testShiftImmInstruction("srli", "SRLI"));
    tests.addAll(testShiftImmInstruction("srai", "SRAI"));
    tests.addAll(testLoadInstruction("lb", "LB", "sb", 8));
    tests.addAll(testLoadInstruction("lh", "LH", "sh", 16));
    tests.addAll(testLoadInstruction("lw", "LW", "sw", 32));
    tests.addAll(testLoadInstruction("ld", "LD", "sd", 64));
    tests.addAll(testLoadInstruction("lbu", "LBU", "sb", 8));
    tests.addAll(testLoadInstruction("lhu", "LHU", "sh", 16));
    tests.addAll(testLoadInstruction("lwu", "LWU", "sw", 32));
    tests.addAll(testStoreInstruction("sb", "SB", "lb", 8));
    tests.addAll(testStoreInstruction("sh", "SH", "lh", 16));
    tests.addAll(testStoreInstruction("sw", "SW", "lw", 32));
    tests.addAll(testStoreInstruction("sd", "SD", "ld", 64));
    tests.addAll(testEqualityBranchInstruction("beq", "BEQ", true));
    tests.addAll(testEqualityBranchInstruction("bne", "BNE", false));
    tests.addAll(testRelationalBranchInstruction("blt", "BLT", true, false));
    tests.addAll(testRelationalBranchInstruction("bge", "BGE", false, false));
    tests.addAll(testRelationalBranchInstruction("bltu", "BLTU", true, true));
    tests.addAll(testRelationalBranchInstruction("bgeu", "BGEU", false, true));
    tests.addAll(testBinaryRegRegInstruction("addw", "ADDW"));
    tests.addAll(testBinaryRegRegInstruction("subw", "SUBW"));
    tests.addAll(testBinaryRegImmInstruction("addiw", "ADDIW"));
    tests.addAll(sllwCases());
    tests.addAll(srlwCases());
    tests.addAll(srawCases());
    tests.addAll(slliwCases());
    tests.addAll(srliwCases());
    tests.addAll(sraiwCases());
    tests.addAll(luiCases());
    tests.addAll(auipcCases());
    tests.addAll(jalCases());
    tests.addAll(jalrCases());
    return tests;
  }

  private String getInstructionName(IssTestUtils.TestCase testCase) {
    var separator = testCase.id().lastIndexOf('_');
    if (separator <= 0) {
      return testCase.id();
    }
    return testCase.id().substring(0, separator).toLowerCase();
  }

  // Helper methods
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

  private List<IssTestUtils.TestCase> testBinaryRegImmInstruction(String instruction,
                                                                  String testNamePrefix) {
    return buildTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regSrc = b.anyTempReg().sample();
      b.fillRegSigned(regSrc, 64);
      var imm = arbitrarySignedInt(12).sample();
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s, %s", instruction, regDest, regSrc, imm);
      return b.toTestCase(regSrc, regDest);
    });
  }

  private List<IssTestUtils.TestCase> testShiftImmInstruction(String instruction,
                                                              String testNamePrefix) {
    return buildTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regSrc = b.anyTempReg().sample();
      b.fillRegSigned(regSrc, 64);
      var shamt = arbitraryUnsignedInt(6).sample();
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s, %s", instruction, regDest, regSrc, shamt);
      return b.toTestCase(regSrc, regDest);
    });
  }

  private int calculateAlignment(int dataSizeInBits) {
    if (dataSizeInBits <= 0) {
      throw new IllegalArgumentException("Data size must be a positive integer.");
    }

    int dataSizeInBytes = (dataSizeInBits + 7) / 8; // Convert bits to bytes, rounding up
    return Integer.highestOneBit(dataSizeInBytes);
  }

  private List<IssTestUtils.TestCase> testLoadInstruction(String instruction,
                                                          String testNamePrefix,
                                                          String storeInstruction,
                                                          int dataSize) {
    return buildTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var storeReg = b.anyTempReg().sample();
      b.fillRegSigned(storeReg, dataSize);
      var addrReg = b.anyTempReg().sample();
      b.fillReg(addrReg, TEST_DATA_MIN_ADDR, TEST_DATA_MAX_ADDR,
          calculateAlignment(dataSize));
      b.add("%s %s, 0(%s)", storeInstruction, storeReg, addrReg);
      var loadReg = b.anyTempReg().sample();
      b.add("%s %s, 0(%s)", instruction, loadReg, addrReg);
      return b.toTestCase(storeReg, loadReg, addrReg);
    });
  }

  private List<IssTestUtils.TestCase> testStoreInstruction(String instruction,
                                                           String testNamePrefix,
                                                           String loadInstruction,
                                                           int dataSize) {
    return buildTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var storeReg = b.anyTempReg().sample();
      b.fillRegSigned(storeReg, dataSize);
      var addrReg = b.anyTempReg().sample();
      b.fillReg(addrReg, TEST_DATA_MIN_ADDR, TEST_DATA_MAX_ADDR,
          calculateAlignment(dataSize));
      b.add("%s %s, 0(%s)", instruction, storeReg, addrReg);
      var loadReg = b.anyTempReg()
          .filter(reg -> !reg.equals(storeReg)).sample();
      b.add("%s %s, 0(%s)", loadInstruction, loadReg, addrReg);
      return b.toTestCase(storeReg, loadReg, addrReg);
    });
  }

  private List<IssTestUtils.TestCase> testEqualityBranchInstruction(String instruction,
                                                                    String testNamePrefix,
                                                                    boolean branchWhenEqual) {
    return buildTestsWith(id -> {
      var b = new RV64IMVTestBuilder(testNamePrefix + "_" + id);
      var rs1 = b.anyTempReg().sample();
      var rs2 = b.anyTempReg().sample();
      Boolean equal = Arbitraries.of(true, false).sample();
      var val1 = b.fillRegSigned(rs1, 64);
      if (Boolean.TRUE.equals(equal) == branchWhenEqual) {
        b.fillReg(rs2, val1);
      } else {
        var val2 = arbitraryUnsignedInt(64).filter(v -> !v.equals(val1)).sample();
        b.fillReg(rs2, val2);
      }
      var destReg = b.anyTempReg().sample();
      String branchLabel = "branch_target_" + id;
      String endLabel = "end_label_" + id;
      b.add("%s %s, %s, %s", instruction, rs1, rs2, branchLabel);
      b.add("addi %s, x0, 1", destReg);
      b.add("j %s", endLabel);
      b.addLabel(branchLabel);
      b.add("addi %s, x0, 2", destReg);
      b.addLabel(endLabel);
      return b.toTestCase(rs1, rs2, destReg);
    });
  }

  private List<IssTestUtils.TestCase> testRelationalBranchInstruction(String instruction,
                                                                      String testNamePrefix,
                                                                      boolean branchWhenLessThan,
                                                                      boolean unsignedComparison) {
    return buildTestsWith(id -> {
      var b = new RV64IMVTestBuilder(testNamePrefix + "_" + id);
      var rs1 = b.anyTempReg().sample();
      var rs2 = b.anyTempReg().sample();
      Boolean conditionMet = Arbitraries.of(true, false).sample();
      BigInteger val1;
      BigInteger val2;
      if (unsignedComparison) {
        BigInteger MAX_UNSIGNED = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
        BigInteger MIN_UNSIGNED = BigInteger.ZERO;
        if (Boolean.TRUE.equals(conditionMet) == branchWhenLessThan) {
          // Branch is taken
          if (branchWhenLessThan) {
            // Need rs1 < rs2
            val1 = Arbitraries.bigIntegers()
                .between(MIN_UNSIGNED, MAX_UNSIGNED.subtract(BigInteger.ONE)).sample();
            val2 =
                Arbitraries.bigIntegers().between(val1.add(BigInteger.ONE), MAX_UNSIGNED).sample();
          } else {
            // Need rs1 >= rs2
            val1 = Arbitraries.bigIntegers().between(MIN_UNSIGNED, MAX_UNSIGNED).sample();
            val2 = Arbitraries.bigIntegers().between(MIN_UNSIGNED, val1).sample();
          }
        } else {
          // Branch is not taken
          if (branchWhenLessThan) {
            // Need rs1 >= rs2
            val1 = Arbitraries.bigIntegers().between(MIN_UNSIGNED, MAX_UNSIGNED).sample();
            val2 = Arbitraries.bigIntegers().between(MIN_UNSIGNED, val1).sample();
          } else {
            // Need rs1 < rs2
            val1 = Arbitraries.bigIntegers()
                .between(MIN_UNSIGNED, MAX_UNSIGNED.subtract(BigInteger.ONE)).sample();
            val2 =
                Arbitraries.bigIntegers().between(val1.add(BigInteger.ONE), MAX_UNSIGNED).sample();
          }
        }
      } else {
        BigInteger MAX_SIGNED = BigInteger.ONE.shiftLeft(63).subtract(BigInteger.ONE);
        BigInteger MIN_SIGNED = BigInteger.ONE.shiftLeft(63).negate();
        if (Boolean.TRUE.equals(conditionMet) == branchWhenLessThan) {
          // Branch is taken
          if (branchWhenLessThan) {
            // Need rs1 < rs2
            val1 =
                Arbitraries.bigIntegers().between(MIN_SIGNED, MAX_SIGNED.subtract(BigInteger.ONE))
                    .sample();
            val2 = Arbitraries.bigIntegers().between(val1.add(BigInteger.ONE), MAX_SIGNED).sample();
          } else {
            // Need rs1 >= rs2
            val1 = Arbitraries.bigIntegers().between(MIN_SIGNED, MAX_SIGNED).sample();
            val2 = Arbitraries.bigIntegers().between(MIN_SIGNED, val1).sample();
          }
        } else {
          // Branch is not taken
          if (branchWhenLessThan) {
            // Need rs1 >= rs2
            val1 = Arbitraries.bigIntegers().between(MIN_SIGNED, MAX_SIGNED).sample();
            val2 = Arbitraries.bigIntegers().between(MIN_SIGNED, val1).sample();
          } else {
            // Need rs1 < rs2
            val1 =
                Arbitraries.bigIntegers().between(MIN_SIGNED, MAX_SIGNED.subtract(BigInteger.ONE))
                    .sample();
            val2 = Arbitraries.bigIntegers().between(val1.add(BigInteger.ONE), MAX_SIGNED).sample();
          }
        }
      }
      b.fillReg(rs1, val1);
      b.fillReg(rs2, val2);
      var destReg = b.anyTempReg().sample();
      String branchLabel = "branch_target_" + id;
      String endLabel = "end_label_" + id;
      b.add("%s %s, %s, %s", instruction, rs1, rs2, branchLabel);
      b.add("addi %s, x0, 1", destReg);
      b.add("j %s", endLabel);
      b.addLabel(branchLabel);
      b.add("addi %s, x0, 2", destReg);
      b.addLabel(endLabel);
      return b.toTestCase(rs1, rs2, destReg);
    });
  }

  private List<IssTestUtils.TestCase> sllwCases() {
    return buildTestsWith(id -> {
      var b = new RV64IMVTestBuilder("SLLW_" + id);
      var regSrc1 = b.anyTempReg().sample();
      var regSrc2 = b.anyTempReg().sample();
      b.fillRegSigned(regSrc1, 64);
      b.fillReg(regSrc2, arbitraryUnsignedInt(5).sample()); // 5 bits for 32-bit shift
      var regDest = b.anyTempReg().sample();
      b.add("sllw %s, %s, %s", regDest, regSrc1, regSrc2);
      return b.toTestCase(regSrc1, regSrc2, regDest);
    });
  }

  private List<IssTestUtils.TestCase> srlwCases() {
    return buildTestsWith(id -> {
      var b = new RV64IMVTestBuilder("SRLW_" + id);
      var regSrc1 = b.anyTempReg().sample();
      var regSrc2 = b.anyTempReg().sample();
      b.fillRegSigned(regSrc1, 64);
      b.fillReg(regSrc2, arbitraryUnsignedInt(5).sample());
      var regDest = b.anyTempReg().sample();
      b.add("srlw %s, %s, %s", regDest, regSrc1, regSrc2);
      return b.toTestCase(regSrc1, regSrc2, regDest);
    });
  }

  private List<IssTestUtils.TestCase> srawCases() {
    return buildTestsWith(id -> {
      var b = new RV64IMVTestBuilder("SRAW_" + id);
      var regSrc1 = b.anyTempReg().sample();
      var regSrc2 = b.anyTempReg().sample();
      b.fillRegSigned(regSrc1, 64);
      b.fillReg(regSrc2, arbitraryUnsignedInt(5).sample());
      var regDest = b.anyTempReg().sample();
      b.add("sraw %s, %s, %s", regDest, regSrc1, regSrc2);
      return b.toTestCase(regSrc1, regSrc2, regDest);
    });
  }

  private List<IssTestUtils.TestCase> slliwCases() {
    return buildTestsWith(id -> {
      var b = new RV64IMVTestBuilder("SLLIW_" + id);
      var regSrc = b.anyTempReg().sample();
      b.fillRegSigned(regSrc, 64);
      var shamt = arbitraryUnsignedInt(5).sample();
      var regDest = b.anyTempReg().sample();
      b.add("slliw %s, %s, %s", regDest, regSrc, shamt);
      return b.toTestCase(regSrc, regDest);
    });
  }

  private List<IssTestUtils.TestCase> srliwCases() {
    return buildTestsWith(id -> {
      var b = new RV64IMVTestBuilder("SRLIW_" + id);
      var regSrc = b.anyTempReg().sample();
      b.fillRegSigned(regSrc, 64);
      var shamt = arbitraryUnsignedInt(5).sample();
      var regDest = b.anyTempReg().sample();
      b.add("srliw %s, %s, %s", regDest, regSrc, shamt);
      return b.toTestCase(regSrc, regDest);
    });
  }

  private List<IssTestUtils.TestCase> sraiwCases() {
    return buildTestsWith(id -> {
      var b = new RV64IMVTestBuilder("SRAIW_" + id);
      var regSrc = b.anyTempReg().sample();
      b.fillRegSigned(regSrc, 64);
      var shamt = arbitraryUnsignedInt(5).sample();
      var regDest = b.anyTempReg().sample();
      b.add("sraiw %s, %s, %s", regDest, regSrc, shamt);
      return b.toTestCase(regSrc, regDest);
    });
  }

  private List<IssTestUtils.TestCase> luiCases() {
    return buildTestsWith(id -> {
      var b = new RV64IMVTestBuilder("LUI_" + id);
      var destReg = b.anyTempReg().sample();
      var value = arbitraryUnsignedInt(20).sample();
      b.add("lui %s, %s", destReg, value);
      return b.toTestCase(destReg);
    });
  }

  private List<IssTestUtils.TestCase> auipcCases() {
    return buildTestsWith(id -> {
      var b = new RV64IMVTestBuilder("AUIPC_" + id);
      var rd = b.anyTempReg().sample();
      var imm = arbitraryUnsignedInt(20).sample();
      b.add("auipc %s, %s", rd, imm);
      return b.toTestCase(rd);
    });
  }

  private List<IssTestUtils.TestCase> jalCases() {
    return buildTestsWith(id -> {
      var b = new RV64IMVTestBuilder("JAL_" + id);
      var rd = b.anyTempReg().sample();
      String targetLabel = "target_" + id;
      String endLabel = "end_" + id;
      b.add("jal %s, %s", rd, targetLabel);
      b.add("addi %s, x0, 0", rd);
      b.add("j %s", endLabel);
      b.addLabel(targetLabel);
      b.add("addi %s, x0, 1", rd);
      b.addLabel(endLabel);
      return b.toTestCase(rd);
    });
  }

  private List<IssTestUtils.TestCase> jalrCases() {
    return buildTestsWith(id -> {
      var b = new RV64IMVTestBuilder("JALR_" + id);
      var rd = b.anyTempReg().sample();
      var rs1 = b.anyTempReg().sample();
      String targetLabel = "target_" + id;
      String endLabel = "end_" + id;
      b.add("auipc %s, 0", rs1);
      b.add("addi %s, %s, 12", rs1, rs1); // Adjust offset to reach targetLabel
      b.add("jalr %s, 0(%s)", rd, rs1);
      b.add("addi %s, x0, 0", rd);
      b.add("j %s", endLabel);
      b.addLabel(targetLabel);
      b.add("addi %s, x0, 1", rd);
      b.addLabel(endLabel);
      return b.toTestCase(rd, rs1);
    });
  }

}
