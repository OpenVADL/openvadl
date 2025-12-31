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
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Tests the RV64V instructions set.
 */
public class IssRV64VInstrTest extends AbstractIssRiscv64InstrTest {

  private static final String VADL_SPEC = "sys/risc-v/rv64v.vadl";
  private static final int TESTS_PER_INSTRUCTION = 50;
  private static final long VECTOR_SRC_1_ADDR = 0x80300000L;
  private static final long VECTOR_SRC_2_ADDR = 0x80400000L;
  private static final long VECTOR_DEST_ADDR = 0x80500000L;
  private static final int RESULT_REG_START = 3;
  private static final int RESULT_REG_END = 31;

  @Override
  public int getTestPerInstruction() {
    return TESTS_PER_INSTRUCTION;
  }

  @Override
  public String getVadlSpec() {
    return VADL_SPEC;
  }

  @Override
  public Tool simulator() {
    return new Tool("/qemu/build/qemu-system-rv64imv", "-bios");
  }

  @Override
  public Tool reference() {
    return new Tool("/qemu/build/qemu-system-riscv64", "-M spike -cpu rv64,v=true,vlen=1024 -bios");
  }

  @Override
  public Tool compiler() {
    return new Tool("/scripts/compilers/riscv_compiler.py", "-march=rv64imv -mabi=lp64");
  }

  public RV64IMVTestBuilder getBuilder(String testNamePrefix, int id) {
    return new RV64IMVTestBuilder(testNamePrefix + "_" + id);
  }

  private Stream<DynamicTest> testBinaryVecInstr(String instruction, String secondOperand,
                                                 String testNamePrefix,
                                                 Consumer<RV64IMVTestBuilder> fillSecondOperand)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);

      b.configureCpuForVecOps("x1");

      b.add("# fill first source vector");
      b.fillVectorAddr("v1", VECTOR_SRC_1_ADDR, "x1", "x2");

      fillSecondOperand.accept(b);

      b.add("# binary vector instruction");
      b.add("%s v0, v1, %s", instruction, secondOperand);

      b.add("# store result in memory");
      b.storeVectorToMemory("v0", VECTOR_DEST_ADDR, "x2");

      b.add("# load result into registers");
      // we can't use x1 and x2 (as well as x0 of course).
      // therefore, we just miss some values. this can be fixed when using the cosim.
      b.loadArrayToRegs(VECTOR_DEST_ADDR, RESULT_REG_START, RESULT_REG_END, "x2");
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testBinaryVecVecInstr(String instruction, String testNamePrefix)
      throws IOException {
    return testBinaryVecInstr(instruction, "v2", testNamePrefix, b -> {
      b.add("# fill second source vector");
      b.fillVectorAddr("v2", VECTOR_SRC_2_ADDR, "x1", "x2");
    });
  }

  private Stream<DynamicTest> testBinaryVecGprInstr(String instruction, String testNamePrefix)
      throws IOException {
    return testBinaryVecInstr(instruction, "x2", testNamePrefix, b -> {
      b.add("# fill source register (second argument)");
      b.fillReg("x2", arbitraryUnsignedInt(64).sample());
    });
  }

  private Stream<DynamicTest> testBinaryVecImmInstr(String instruction, String testNamePrefix)
      throws IOException {
    var imm = arbitrarySignedInt(5).sample();
    return testBinaryVecInstr(instruction, "" + imm, testNamePrefix, b -> {
      b.add("# immediate value: %d", imm);
    });
  }


// Test methods using helper functions

  @TestFactory
  Stream<DynamicTest> vaddvv() throws IOException {
    return testBinaryVecVecInstr("vadd.vv", "VADD.VV");
  }

  @TestFactory
  Stream<DynamicTest> vaddvx() throws IOException {
    return testBinaryVecGprInstr("vadd.vx", "VADD.VX");
  }

  @TestFactory
  Stream<DynamicTest> vaddvi() throws IOException {
    return testBinaryVecImmInstr("vadd.vi", "VADD.VI");
  }
}
