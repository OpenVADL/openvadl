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

import static vadl.TestUtils.arbitrarySignedInt;
import static vadl.TestUtils.arbitraryUnsignedInt;

import java.io.IOException;
import java.math.BigInteger;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.iss.AsmTestBuilder;

/**
 * Tests the RV64I instructions set.
 */
public class IssRV64IInstrTest extends AbstractIssRiscv64InstrTest {

  private static final String VADL_SPEC = "sys/risc-v/rv64im.vadl";
  private static final int TESTS_PER_INSTRUCTION = 50;

  @Override
  public int getTestPerInstruction() {
    return TESTS_PER_INSTRUCTION;
  }

  @Override
  public String getVadlSpec() {
    return VADL_SPEC;
  }

  public AsmTestBuilder getBuilder(String testNamePrefix, int id) {
    return new RV64IMTestBuilder(testNamePrefix + "_" + id);
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

  private Stream<DynamicTest> testBinaryRegImmInstruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regSrc = b.anyTempReg().sample();
      b.fillRegSigned(regSrc, 64);
      var imm = arbitrarySignedInt(12).sample();
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s, %s", instruction, regDest, regSrc, imm);
      return b.toTestCase(regSrc, regDest);
    });
  }

  private Stream<DynamicTest> testShiftImmInstruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
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

  private Stream<DynamicTest> testLoadInstruction(String instruction, String testNamePrefix,
                                                  String storeInstruction, int dataSize)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var storeReg = b.anyTempReg().sample();
      b.fillRegSigned(storeReg, dataSize);
      var addrReg = b.anyTempReg().sample();
      b.fillReg(addrReg, BigInteger.valueOf(0x80000100L), BigInteger.valueOf(0x800F0000L),
          calculateAlignment(dataSize));
      b.add("%s %s, 0(%s)", storeInstruction, storeReg, addrReg);
      var loadReg = b.anyTempReg().sample();
      b.add("%s %s, 0(%s)", instruction, loadReg, addrReg);
      return b.toTestCase(storeReg, loadReg, addrReg);
    });
  }

  private Stream<DynamicTest> testStoreInstruction(String instruction, String testNamePrefix,
                                                   String loadInstruction, int dataSize)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var storeReg = b.anyTempReg().sample();
      b.fillRegSigned(storeReg, dataSize);
      var addrReg = b.anyTempReg().sample();
      b.fillReg(addrReg, BigInteger.valueOf(0x80000100L), BigInteger.valueOf(0x800F0000L),
          calculateAlignment(dataSize));
      b.add("%s %s, 0(%s)", instruction, storeReg, addrReg);
      var loadReg = b.anyTempReg()
          .filter(reg -> !reg.equals(storeReg)).sample();
      b.add("%s %s, 0(%s)", loadInstruction, loadReg, addrReg);
      return b.toTestCase(storeReg, loadReg, addrReg);
    });
  }

  private Stream<DynamicTest> testEqualityBranchInstruction(String instruction,
                                                            String testNamePrefix,
                                                            boolean branchWhenEqual)
      throws IOException {
    return runTestsWith(id -> {
      var b = new RV64IMTestBuilder(testNamePrefix + "_" + id);
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

  private Stream<DynamicTest> testRelationalBranchInstruction(String instruction,
                                                              String testNamePrefix,
                                                              boolean branchWhenLessThan,
                                                              boolean unsignedComparison)
      throws IOException {
    return runTestsWith(id -> {
      var b = new RV64IMTestBuilder(testNamePrefix + "_" + id);
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

// Test methods using helper functions

  @TestFactory
  Stream<DynamicTest> add() throws IOException {
    return testBinaryRegRegInstruction("add", "ADD");
  }

  @TestFactory
  Stream<DynamicTest> sub() throws IOException {
    return testBinaryRegRegInstruction("sub", "SUB");
  }

  @TestFactory
  Stream<DynamicTest> and() throws IOException {
    return testBinaryRegRegInstruction("and", "AND");
  }

  @TestFactory
  Stream<DynamicTest> or() throws IOException {
    return testBinaryRegRegInstruction("or", "OR");
  }

  @TestFactory
  Stream<DynamicTest> xor() throws IOException {
    return testBinaryRegRegInstruction("xor", "XOR");
  }

  @TestFactory
  Stream<DynamicTest> slt() throws IOException {
    return testBinaryRegRegInstruction("slt", "SLT");
  }

  @TestFactory
  Stream<DynamicTest> sltu() throws IOException {
    return testBinaryRegRegInstruction("sltu", "SLTU");
  }

  @TestFactory
  Stream<DynamicTest> addi() throws IOException {
    return testBinaryRegImmInstruction("addi", "ADDI");
  }

  @TestFactory
  Stream<DynamicTest> andi() throws IOException {
    return testBinaryRegImmInstruction("andi", "ANDI");
  }

  @TestFactory
  Stream<DynamicTest> ori() throws IOException {
    return testBinaryRegImmInstruction("ori", "ORI");
  }

  @TestFactory
  Stream<DynamicTest> xori() throws IOException {
    return testBinaryRegImmInstruction("xori", "XORI");
  }

  @TestFactory
  Stream<DynamicTest> slti() throws IOException {
    return testBinaryRegImmInstruction("slti", "SLTI");
  }

  @TestFactory
  Stream<DynamicTest> sltiu() throws IOException {
    return testBinaryRegImmInstruction("sltiu", "SLTIU");
  }

  @TestFactory
  Stream<DynamicTest> slli() throws IOException {
    return testShiftImmInstruction("slli", "SLLI");
  }

  @TestFactory
  Stream<DynamicTest> srli() throws IOException {
    return testShiftImmInstruction("srli", "SRLI");
  }

  @TestFactory
  Stream<DynamicTest> srai() throws IOException {
    return testShiftImmInstruction("srai", "SRAI");
  }

  @TestFactory
  Stream<DynamicTest> lb() throws IOException {
    return testLoadInstruction("lb", "LB", "sb", 8);
  }

  @TestFactory
  Stream<DynamicTest> lh() throws IOException {
    return testLoadInstruction("lh", "LH", "sh", 16);
  }

  @TestFactory
  Stream<DynamicTest> lw() throws IOException {
    return testLoadInstruction("lw", "LW", "sw", 32);
  }

  @TestFactory
  Stream<DynamicTest> ld() throws IOException {
    return testLoadInstruction("ld", "LD", "sd", 64);
  }

  @TestFactory
  Stream<DynamicTest> lbu() throws IOException {
    return testLoadInstruction("lbu", "LBU", "sb", 8);
  }

  @TestFactory
  Stream<DynamicTest> lhu() throws IOException {
    return testLoadInstruction("lhu", "LHU", "sh", 16);
  }

  @TestFactory
  Stream<DynamicTest> lwu() throws IOException {
    return testLoadInstruction("lwu", "LWU", "sw", 32);
  }

  @TestFactory
  Stream<DynamicTest> sb() throws IOException {
    return testStoreInstruction("sb", "SB", "lb", 8);
  }

  @TestFactory
  Stream<DynamicTest> sh() throws IOException {
    return testStoreInstruction("sh", "SH", "lh", 16);
  }

  @TestFactory
  Stream<DynamicTest> sw() throws IOException {
    return testStoreInstruction("sw", "SW", "lw", 32);
  }

  @TestFactory
  Stream<DynamicTest> sd() throws IOException {
    return testStoreInstruction("sd", "SD", "ld", 64);
  }

  @TestFactory
  Stream<DynamicTest> beq() throws IOException {
    return testEqualityBranchInstruction("beq", "BEQ", true);
  }

  @TestFactory
  Stream<DynamicTest> bne() throws IOException {
    return testEqualityBranchInstruction("bne", "BNE", false);
  }

  @TestFactory
  Stream<DynamicTest> blt() throws IOException {
    return testRelationalBranchInstruction("blt", "BLT", true, false);
  }

  @TestFactory
  Stream<DynamicTest> bge() throws IOException {
    return testRelationalBranchInstruction("bge", "BGE", false, false);
  }

  @TestFactory
  Stream<DynamicTest> bltu() throws IOException {
    return testRelationalBranchInstruction("bltu", "BLTU", true, true);
  }

  @TestFactory
  Stream<DynamicTest> bgeu() throws IOException {
    return testRelationalBranchInstruction("bgeu", "BGEU", false, true);
  }

  @TestFactory
  Stream<DynamicTest> addw() throws IOException {
    return testBinaryRegRegInstruction("addw", "ADDW");
  }

  @TestFactory
  Stream<DynamicTest> subw() throws IOException {
    return testBinaryRegRegInstruction("subw", "SUBW");
  }

  @TestFactory
  Stream<DynamicTest> addiw() throws IOException {
    return testBinaryRegImmInstruction("addiw", "ADDIW");
  }

  @TestFactory
  Stream<DynamicTest> sllw() throws IOException {
    return runTestsWith(id -> {
      var b = new RV64IMTestBuilder("SLLW_" + id);
      var regSrc1 = b.anyTempReg().sample();
      var regSrc2 = b.anyTempReg().sample();
      b.fillRegSigned(regSrc1, 64);
      b.fillReg(regSrc2, arbitraryUnsignedInt(5).sample()); // 5 bits for 32-bit shift
      var regDest = b.anyTempReg().sample();
      b.add("sllw %s, %s, %s", regDest, regSrc1, regSrc2);
      return b.toTestCase(regSrc1, regSrc2, regDest);
    });
  }

  @TestFactory
  Stream<DynamicTest> srlw() throws IOException {
    return runTestsWith(id -> {
      var b = new RV64IMTestBuilder("SRLW_" + id);
      var regSrc1 = b.anyTempReg().sample();
      var regSrc2 = b.anyTempReg().sample();
      b.fillRegSigned(regSrc1, 64);
      b.fillReg(regSrc2, arbitraryUnsignedInt(5).sample());
      var regDest = b.anyTempReg().sample();
      b.add("srlw %s, %s, %s", regDest, regSrc1, regSrc2);
      return b.toTestCase(regSrc1, regSrc2, regDest);
    });
  }

  @TestFactory
  Stream<DynamicTest> sraw() throws IOException {
    return runTestsWith(id -> {
      var b = new RV64IMTestBuilder("SRAW_" + id);
      var regSrc1 = b.anyTempReg().sample();
      var regSrc2 = b.anyTempReg().sample();
      b.fillRegSigned(regSrc1, 64);
      b.fillReg(regSrc2, arbitraryUnsignedInt(5).sample());
      var regDest = b.anyTempReg().sample();
      b.add("sraw %s, %s, %s", regDest, regSrc1, regSrc2);
      return b.toTestCase(regSrc1, regSrc2, regDest);
    });
  }

  @TestFactory
  Stream<DynamicTest> slliw() throws IOException {
    return runTestsWith(id -> {
      var b = new RV64IMTestBuilder("SLLIW_" + id);
      var regSrc = b.anyTempReg().sample();
      b.fillRegSigned(regSrc, 64);
      var shamt = arbitraryUnsignedInt(5).sample();
      var regDest = b.anyTempReg().sample();
      b.add("slliw %s, %s, %s", regDest, regSrc, shamt);
      return b.toTestCase(regSrc, regDest);
    });
  }

  @TestFactory
  Stream<DynamicTest> srliw() throws IOException {
    return runTestsWith(id -> {
      var b = new RV64IMTestBuilder("SRLIW_" + id);
      var regSrc = b.anyTempReg().sample();
      b.fillRegSigned(regSrc, 64);
      var shamt = arbitraryUnsignedInt(5).sample();
      var regDest = b.anyTempReg().sample();
      b.add("srliw %s, %s, %s", regDest, regSrc, shamt);
      return b.toTestCase(regSrc, regDest);
    });
  }

  @TestFactory
  Stream<DynamicTest> sraiw() throws IOException {
    return runTestsWith(id -> {
      var b = new RV64IMTestBuilder("SRAIW_" + id);
      var regSrc = b.anyTempReg().sample();
      b.fillRegSigned(regSrc, 64);
      var shamt = arbitraryUnsignedInt(5).sample();
      var regDest = b.anyTempReg().sample();
      b.add("sraiw %s, %s, %s", regDest, regSrc, shamt);
      return b.toTestCase(regSrc, regDest);
    });
  }


// The following instructions are unique and might not fit into the above helpers, so we'll keep them as is.

  @TestFactory
  Stream<DynamicTest> lui() throws IOException {
    return runTestsWith((id) -> {
      var b = new RV64IMTestBuilder("LUI_" + id);
      var destReg = b.anyTempReg().sample();
      var value = arbitraryUnsignedInt(20).sample();
      b.add("lui %s, %s", destReg, value);
      return b.toTestCase(destReg);
    });
  }

  @TestFactory
  Stream<DynamicTest> auipc() throws IOException {
    return runTestsWith(id -> {
      var b = new RV64IMTestBuilder("AUIPC_" + id);
      var rd = b.anyTempReg().sample();
      var imm = arbitraryUnsignedInt(20).sample();
      b.add("auipc %s, %s", rd, imm);
      return b.toTestCase(rd);
    });
  }

  @TestFactory
  Stream<DynamicTest> jal() throws IOException {
    return runTestsWith(id -> {
      var b = new RV64IMTestBuilder("JAL_" + id);
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

  @TestFactory
  Stream<DynamicTest> jalr() throws IOException {
    return runTestsWith(id -> {
      var b = new RV64IMTestBuilder("JALR_" + id);
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
