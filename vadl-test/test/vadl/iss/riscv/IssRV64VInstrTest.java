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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.iss.IssTestUtils;

/**
 * Tests the RV64V instructions set.
 */
public class IssRV64VInstrTest extends AbstractIssRiscv64InstrTest {

  private static final String VADL_SPEC = "sys/risc-v/rv64v.vadl";
  private static final int TESTS_PER_INSTRUCTION = 25;
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

  private IssTestUtils.TestCase createBinaryVecInstrTest(RV64IMVTestBuilder b, String instruction,
                                                         String secondOperand,
                                                         boolean fillDest,
                                                         boolean flipArgs,
                                                         Consumer<RV64IMVTestBuilder> fillSecondOperand) {
    b.configureCpuForVecOps("x1");

    if (fillDest) {
      b.add("# fill destination vector");
      b.fillVectorAddr("v0", VECTOR_DEST_ADDR, "x1", "x2");
    }

    b.add("# fill first source vector");
    b.fillVectorAddr("v1", VECTOR_SRC_1_ADDR, "x1", "x2");

    fillSecondOperand.accept(b);

    b.add("# binary vector instruction");
    if (flipArgs) {
      b.add("%s v0, %s, v1", instruction, secondOperand);
    } else {
      b.add("%s v0, v1, %s", instruction, secondOperand);
    }

    b.add("# store result in memory");
    b.storeVectorToMemory("v0", VECTOR_DEST_ADDR, "x2");

    b.add("# load result into registers");
    // we can't use x1 and x2 (as well as x0 of course).
    // therefore, we just miss some values. this can be fixed when using the cosim.
    b.loadArrayToRegs(VECTOR_DEST_ADDR, RESULT_REG_START, RESULT_REG_END, "x2");
    return b.toTestCase();
  }

  private IssTestUtils.TestCase createBinaryVecVecInstrTest(RV64IMVTestBuilder builder,
                                                            String instruction, boolean fillDest) {
    return createBinaryVecInstrTest(builder, instruction, "v2", fillDest, false, b -> {
      b.add("# fill second source vector");
      b.fillVectorAddr("v2", VECTOR_SRC_2_ADDR, "x1", "x2");
    });
  }

  private IssTestUtils.TestCase createBinaryVecScalarInstrTest(RV64IMVTestBuilder builder,
                                                               String instruction, boolean fillDest,
                                                               boolean flipArgs) {
    return createBinaryVecInstrTest(builder, instruction, "x2", fillDest, flipArgs, b -> {
      b.add("# fill source register (second argument)");
      b.fillReg("x2", arbitraryUnsignedInt(64).sample());
    });
  }

  private IssTestUtils.TestCase createBinaryVecImmInstrTest(RV64IMVTestBuilder builder,
                                                            String instruction, boolean fillDest) {
    var imm = arbitrarySignedInt(5).sample();
    return createBinaryVecInstrTest(builder, instruction, "" + imm, fillDest, false, b ->
        b.add("# immediate value: %d", imm));
  }

  private IssTestUtils.TestCase createReductionVecInstrTest(RV64IMVTestBuilder b,
                                                            String instruction) {
    b.configureCpuForVecOps("x1");

    b.add("# fill destination vector");
    b.fillVectorAddr("v0", VECTOR_DEST_ADDR, "x1", "x2");

    b.add("# fill reduction source vector (vs2)");
    b.fillVectorAddr("v1", VECTOR_SRC_1_ADDR, "x1", "x2");

    b.add("# fill reduction seed vector (vs1)");
    b.fillVectorAddr("v2", VECTOR_SRC_2_ADDR, "x1", "x2");

    b.add("# reduction vector instruction");
    b.add("%s.vs v0, v1, v2", instruction);

    b.add("# store result in memory");
    b.storeVectorToMemory("v0", VECTOR_DEST_ADDR, "x2");

    b.add("# load result into registers");
    b.loadArrayToRegs(VECTOR_DEST_ADDR, RESULT_REG_START, RESULT_REG_END, "x2");
    return b.toTestCase();
  }

  private Stream<DynamicTest> testBinaryVecInstr(String instruction,
                                                 boolean v, boolean x, boolean i)
      throws IOException {
    return testBinaryVecInstr(instruction, false, false, v, x, i);
  }

  /**
   * Generates dynamic tests for binary vector instructions. The method creates and
   * executes multiple test cases for combinations of vector-vector, vector-scalar,
   * and vector-immediate instructions based on the provided parameters.
   *
   * @param instruction the base instruction name (e.g., "vadd", "vsub").
   * @param fillDest    a flag indicating whether the destination register should be pre-filled
   *                    with specific test data before execution.
   * @param flipArgs    a flag indicating whether the instruction's arguments should be
   *                    flipped in the generated test cases, such that the GPR argument is
   *                    the first one and the vector register argument is the second one.
   * @param v           a flag to indicate that tests for vector-vector (VV) instructions should be generated.
   * @param x           a flag to indicate that tests for vector-scalar (VX) instructions should be generated.
   * @param i           a flag to indicate that tests for vector-immediate (VI) instructions should be generated.
   * @return a stream of dynamic tests for the specified combinations of binary vector instructions.
   * @throws IOException if an error occurs during test case generation or execution.
   */
  private Stream<DynamicTest> testBinaryVecInstr(String instruction,
                                                 boolean fillDest, boolean flipArgs,
                                                 boolean v, boolean x, boolean i)
      throws IOException {
    var testNamePrefix = instruction.toUpperCase();
    List<Function<Integer, IssTestUtils.TestCase>> generators = new ArrayList<>();
    if (v) {
      generators.add((id) -> {
        var builder = getBuilder(testNamePrefix + ".VV", id);
        return createBinaryVecVecInstrTest(builder, instruction + ".vv", fillDest);
      });
    }
    if (x) {
      generators.add((id) -> {
        var builder = getBuilder(testNamePrefix + ".VX", id);
        return createBinaryVecScalarInstrTest(builder, instruction + ".vx", fillDest, flipArgs);
      });
    }
    if (i) {
      generators.add((id) -> {
        var builder = getBuilder(testNamePrefix + ".VI", id);
        return createBinaryVecImmInstrTest(builder, instruction + ".vi", fillDest);
      });
    }
    return runTestsWith(generators);
  }


// Test methods using helper functions

  @TestFactory
  Stream<DynamicTest> vadd() throws IOException {
    return testBinaryVecInstr("vadd", true, true, true);
  }

  @TestFactory
  Stream<DynamicTest> vsub() throws IOException {
    return testBinaryVecInstr("vsub", true, true, false);
  }

  @TestFactory
  Stream<DynamicTest> vmul() throws IOException {
    return testBinaryVecInstr("vmul", true, true, false);
  }

  @TestFactory
  Stream<DynamicTest> vmulh() throws IOException {
    return testBinaryVecInstr("vmulh", true, true, false);
  }

  @TestFactory
  Stream<DynamicTest> vmulhu() throws IOException {
    return testBinaryVecInstr("vmulhu", true, true, false);
  }

  @TestFactory
  Stream<DynamicTest> vmulhsu() throws IOException {
    return testBinaryVecInstr("vmulhsu", true, true, false);
  }

  @TestFactory
  Stream<DynamicTest> vdiv() throws IOException {
    return testBinaryVecInstr("vdiv", true, true, false);
  }

  @TestFactory
  Stream<DynamicTest> vdivu() throws IOException {
    return testBinaryVecInstr("vdivu", true, true, false);
  }

  @TestFactory
  Stream<DynamicTest> vrem() throws IOException {
    return testBinaryVecInstr("vrem", true, true, false);
  }

  @TestFactory
  Stream<DynamicTest> vremu() throws IOException {
    return testBinaryVecInstr("vremu", true, true, false);
  }

  @TestFactory
  Stream<DynamicTest> vminu() throws IOException {
    return testBinaryVecInstr("vminu", true, true, false);
  }

  @TestFactory
  Stream<DynamicTest> vmin() throws IOException {
    return testBinaryVecInstr("vmin", true, true, false);
  }

  @TestFactory
  Stream<DynamicTest> vmaxu() throws IOException {
    return testBinaryVecInstr("vmaxu", true, true, false);
  }

  @TestFactory
  Stream<DynamicTest> vmax() throws IOException {
    return testBinaryVecInstr("vmax", true, true, false);
  }

  @TestFactory
  Stream<DynamicTest> vand() throws IOException {
    return testBinaryVecInstr("vand", true, true, true);
  }

  @TestFactory
  Stream<DynamicTest> vor() throws IOException {
    return testBinaryVecInstr("vor", true, true, true);
  }

  @TestFactory
  Stream<DynamicTest> vm() throws IOException {
    return testBinaryVecInstr("vmseq", true, true, true);
  }

  @TestFactory
  Stream<DynamicTest> vxor() throws IOException {
    return testBinaryVecInstr("vxor", true, true, true);
  }

  @TestFactory
  Stream<DynamicTest> vmadd() throws IOException {
    return testBinaryVecInstr("vmadd", true, true, true, true, false);
  }

  @TestFactory
  Stream<DynamicTest> vnmsub() throws IOException {
    return testBinaryVecInstr("vnmsub", true, true, true, true, false);
  }

  @TestFactory
  Stream<DynamicTest> vmacc() throws IOException {
    return testBinaryVecInstr("vmacc", true, true, true, true, false);
  }

  @TestFactory
  Stream<DynamicTest> vnmsac() throws IOException {
    return testBinaryVecInstr("vnmsac", true, true, true, true, false);
  }

  @TestFactory
  Stream<DynamicTest> vredsum() throws IOException {
    return runTestsWith((id) -> createReductionVecInstrTest(getBuilder("VREDSUM.VS", id), "vredsum"));
  }

  @TestFactory
  Stream<DynamicTest> vredand() throws IOException {
    return runTestsWith((id) -> createReductionVecInstrTest(getBuilder("VREDAND.VS", id), "vredand"));
  }

  @TestFactory
  Stream<DynamicTest> vredor() throws IOException {
    return runTestsWith((id) -> createReductionVecInstrTest(getBuilder("VREDOR.VS", id), "vredor"));
  }

  @TestFactory
  Stream<DynamicTest> vredxor() throws IOException {
    return runTestsWith((id) -> createReductionVecInstrTest(getBuilder("VREDXOR.VS", id), "vredxor"));
  }

}
