// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.aarch64;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestMethodOrder;
import vadl.iss.IssTestUtils;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.ViamUtils;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.RegisterTensor;

/**
 * Tests a core subset of AArch64 SVE instructions.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IssA64SVEInstrTest extends AbstractIssAarch64InstrTest {

  private static final String VADL_SPEC = "sys/aarch64/vprocessor.vadl";
  private static final int TESTS_PER_INSTRUCTION = 10;

  private static final long VECTOR_SRC_1_ADDR = 0x40400000L;
  private static final long VECTOR_SRC_2_ADDR = 0x40410000L;
  private static final long VECTOR_DEST_ADDR = 0x40420000L;

  private static final int RESULT_REG_START = 3;
  private static final int RESULT_REG_END = 30;

  private InstructionSetArchitecture isa;
  private int vlBits = -1;
  private int vectorChunkCount = -1;
  private String referenceArgs;

  @Override
  public int getTestPerInstruction() {
    return TESTS_PER_INSTRUCTION;
  }

  @Override
  public String getVadlSpec() {
    return VADL_SPEC;
  }

  @Override
  public Tool reference() {
    Assertions.assertNotNull(referenceArgs,
        "SVE test setup did not run yet (reference args are not initialized).");
    return new Tool("/qemu/build/qemu-system-aarch64", referenceArgs);
  }

  @Override
  public Tool compiler() {
    return new Tool("/scripts/compilers/aarch64_compiler.py", "-march=armv8-a+sve2");
  }

  @Override
  public A64SVETestBuilder getBuilder(String testNamePrefix, int id) {
    return new A64SVETestBuilder(testNamePrefix + "_" + id);
  }

  @BeforeEach
  void setup() throws IOException, DuplicatedPassKeyException {
    if (isa != null) {
      return;
    }

    var setup = setupPassManagerAndRunSpec(getVadlSpec(), PassOrders.iss(getConfiguration(false)));
    isa = setup.specification().isa().orElseThrow(() ->
        new IllegalStateException("Specification has no ISA."));

    var z = findRegisterTensorBySimpleName("Z");
    var p = findRegisterTensorBySimpleName("P");

    vlBits = z.resultType().bitWidth();
    Assertions.assertTrue(vlBits > 0, "SVE VL must be > 0");
    Assumptions.assumeTrue(vlBits % 128 == 0,
        "Skipping SVE ISS test suite: spec VL=" + vlBits
            + " is not supported by upstream QEMU (requires 128-bit steps).");
    Assertions.assertEquals(vlBits / 8, p.resultType().bitWidth(),
        "Predicate register width does not match PL = VL / 8.");

    vectorChunkCount = vlBits / 64;
    Assertions.assertTrue(vectorChunkCount > 0,
        "SVE VL must allow at least one 64-bit result chunk.");
    Assumptions.assumeTrue(vectorChunkCount <= (RESULT_REG_END - RESULT_REG_START + 1),
        "Skipping SVE ISS test suite: not enough scalar registers to compare full vector result.");

    referenceArgs = String.format(
        "-M virt -cpu max,sve%d=on,pmu=off -semihosting -accel tcg,one-insn-per-tb=on -kernel",
        vlBits);
  }

  private RegisterTensor findRegisterTensorBySimpleName(String simpleName) {
    var matches = ViamUtils.findDefinitionsByFilter(isa,
            d -> d instanceof RegisterTensor rt && rt.simpleName().equals(simpleName))
        .stream()
        .map(d -> (RegisterTensor) d)
        .toList();
    Assertions.assertEquals(1, matches.size(),
        "Expected exactly one register tensor with simple name " + simpleName);
    return matches.getFirst();
  }

  private IssTestUtils.TestCase createPredicatedBinaryInstrTest(A64SVETestBuilder b,
                                                                String instruction,
                                                                String elemSuffix,
                                                                int id) {
    var regs = randomRegs("pred-" + instruction + "-" + elemSuffix, id, true);
    b.configureCpuForSveOps("x1", vlBits);

    b.fillMemory64(VECTOR_DEST_ADDR, vectorChunkCount, "x1", "x2");
    b.loadZFromMemory(regs.zd(), VECTOR_DEST_ADDR, "x1");

    b.fillMemory64(VECTOR_SRC_1_ADDR, vectorChunkCount, "x1", "x2");
    b.loadZFromMemory(regs.zm(), VECTOR_SRC_1_ADDR, "x1");

    b.setPredicateAllTrue(regs.pg(), elemSuffix);
    b.add("%s %s.%s, %s/m, %s.%s, %s.%s",
        instruction, regs.zd(), elemSuffix, regs.pg(), regs.zd(), elemSuffix, regs.zm(),
        elemSuffix);

    b.storeZToMemory(regs.zd(), VECTOR_DEST_ADDR, "x1");
    b.loadMemory64ToRegs(VECTOR_DEST_ADDR, vectorChunkCount, RESULT_REG_START, "x1");
    return b.toTestCase();
  }

  private IssTestUtils.TestCase createUnpredicatedBinaryInstrTest(A64SVETestBuilder b,
                                                                  String instruction,
                                                                  String elemSuffix,
                                                                  int id) {
    var regs = randomRegs("unpred-" + instruction + "-" + elemSuffix, id, false);
    b.configureCpuForSveOps("x1", vlBits);

    b.fillMemory64(VECTOR_SRC_1_ADDR, vectorChunkCount, "x1", "x2");
    b.loadZFromMemory(regs.zn(), VECTOR_SRC_1_ADDR, "x1");

    b.fillMemory64(VECTOR_SRC_2_ADDR, vectorChunkCount, "x1", "x2");
    b.loadZFromMemory(regs.zm(), VECTOR_SRC_2_ADDR, "x1");

    b.add("%s %s.%s, %s.%s, %s.%s", instruction, regs.zd(), elemSuffix, regs.zn(), elemSuffix,
        regs.zm(), elemSuffix);

    b.storeZToMemory(regs.zd(), VECTOR_DEST_ADDR, "x1");
    b.loadMemory64ToRegs(VECTOR_DEST_ADDR, vectorChunkCount, RESULT_REG_START, "x1");
    return b.toTestCase();
  }

  private IssTestUtils.TestCase createUnpredicatedExactOverlapTest(A64SVETestBuilder b) {
    var zdAndZn = "z5";
    var zm = "z6";
    b.configureCpuForSveOps("x1", vlBits);

    b.fillMemory64(VECTOR_SRC_1_ADDR, vectorChunkCount, "x1", "x2");
    b.loadZFromMemory(zdAndZn, VECTOR_SRC_1_ADDR, "x1");

    b.fillMemory64(VECTOR_SRC_2_ADDR, vectorChunkCount, "x1", "x2");
    b.loadZFromMemory(zm, VECTOR_SRC_2_ADDR, "x1");

    b.add("add %s.b, %s.b, %s.b", zdAndZn, zdAndZn, zm);
    b.storeZToMemory(zdAndZn, VECTOR_DEST_ADDR, "x1");
    b.loadMemory64ToRegs(VECTOR_DEST_ADDR, vectorChunkCount, RESULT_REG_START, "x1");
    return b.toTestCase();
  }

  private IssTestUtils.TestCase createReductionInstrTest(A64SVETestBuilder b,
                                                         String instruction,
                                                         String elemSuffix,
                                                         boolean extendedAddReduction,
                                                         int id) {
    var regs = randomRegs("red-" + instruction + "-" + elemSuffix, id, true);
    b.configureCpuForSveOps("x1", vlBits);

    b.fillMemory64(VECTOR_DEST_ADDR, vectorChunkCount, "x1", "x2");
    b.loadZFromMemory(regs.zd(), VECTOR_DEST_ADDR, "x1");

    b.fillMemory64(VECTOR_SRC_1_ADDR, vectorChunkCount, "x1", "x2");
    b.loadZFromMemory(regs.zn(), VECTOR_SRC_1_ADDR, "x1");

    b.setPredicateAllTrue(regs.pg(), elemSuffix);

    if (extendedAddReduction) {
      b.add("%s d%d, %s, %s.%s", instruction, regs.zdIndex(), regs.pg(), regs.zn(), elemSuffix);
    } else {
      b.add("%s %s, %s, %s.%s", instruction, scalarDestFor(elemSuffix, regs.zdIndex()),
          regs.pg(), regs.zn(), elemSuffix);
    }

    b.storeZToMemory(regs.zd(), VECTOR_DEST_ADDR, "x1");
    b.loadMemory64ToRegs(VECTOR_DEST_ADDR, vectorChunkCount, RESULT_REG_START, "x1");
    return b.toTestCase();
  }

  private String scalarDestFor(String elemSuffix, int regIndex) {
    return switch (elemSuffix) {
      case "b" -> "b" + regIndex;
      case "h" -> "h" + regIndex;
      case "s" -> "s" + regIndex;
      case "d" -> "d" + regIndex;
      default -> throw new IllegalArgumentException("Unsupported element suffix: " + elemSuffix);
    };
  }

  private IssTestUtils.TestCase createUmovInstrTest(A64SVETestBuilder b, int id) {
    var regs = randomRegs("umov", id, false);
    var xdst = 9 + Math.floorMod(id, 10);
    b.configureCpuForSveOps("x1", vlBits);

    b.fillMemory64(VECTOR_SRC_1_ADDR, vectorChunkCount, "x1", "x2");
    b.loadZFromMemory(regs.zn(), VECTOR_SRC_1_ADDR, "x1");

    b.add("umov x%d, v%d.d[0]", xdst, regs.znIndex());
    return b.toTestCase();
  }

  private record RandomRegs(int zdIndex, int znIndex, int zmIndex, int pgIndex) {
    String zd() {
      return "z" + zdIndex;
    }

    String zn() {
      return "z" + znIndex;
    }

    String zm() {
      return "z" + zmIndex;
    }

    String pg() {
      return "p" + pgIndex;
    }
  }

  private RandomRegs randomRegs(String salt, int id, boolean withPredicate) {
    var seed = 0x5EED5EEDL ^ (((long) id) << 32) ^ salt.hashCode();
    var rnd = new SplittableRandom(seed);
    var zd = rnd.nextInt(32);
    var zn = rnd.nextInt(32);
    while (zn == zd) {
      zn = rnd.nextInt(32);
    }
    var zm = rnd.nextInt(32);
    while (zm == zd || zm == zn) {
      zm = rnd.nextInt(32);
    }
    var pg = withPredicate ? rnd.nextInt(8) : 0;
    return new RandomRegs(zd, zn, zm, pg);
  }

  @Test
  @Order(1)
  void setupInstructionTests() throws IOException {
    initializeInstructionBatch(this::buildInstructionTestGroups);
  }

  @TestFactory
  @Order(2)
  Stream<DynamicNode> buildInstructionTests() throws IOException {
    initializeInstructionBatch(this::buildInstructionTestGroups);
    return buildInstructionTestContainers();
  }

  private List<InstructionTestGroup> buildInstructionTestGroups() {
    return List.of(
        instructionGroup("svePredAdd", predicatedBinaryInstrTests("add", "b", "h", "s", "d")),
        instructionGroup("svePredSub", predicatedBinaryInstrTests("sub", "b", "h", "s", "d")),
        instructionGroup("svePredMul", predicatedBinaryInstrTests("mul", "b", "h", "s", "d")),
        instructionGroup("sveUnpredAdd", unpredicatedBinaryInstrTests("add", "b", "h", "s", "d")),
        instructionGroup("sveUnpredSub", unpredicatedBinaryInstrTests("sub", "b", "h", "s", "d")),
        instructionGroup("sveUnpredMul", unpredicatedBinaryInstrTests("mul", "b", "h", "s", "d")),
        instructionGroup("sveUnpredAddExactOverlap",
            buildTestsWith(id -> createUnpredicatedExactOverlapTest(
                getBuilder("SVE.UNPRED.ADD.OVERLAP", id)
            ))),
        instructionGroup("sveFoldSaddv", reductionInstrTests("saddv", true, "b", "h", "s")),
        instructionGroup("sveFoldUaddv", reductionInstrTests("uaddv", true, "b", "h", "s")),
        instructionGroup("sveFoldAndv", reductionInstrTests("andv", false, "b", "h", "s", "d")),
        instructionGroup("sveFoldOrv", reductionInstrTests("orv", false, "b", "h", "s", "d")),
        instructionGroup("sveFoldEorv", reductionInstrTests("eorv", false, "b", "h", "s", "d")),
        instructionGroup("sveUmov",
            buildTestsWith((id) -> createUmovInstrTest(getBuilder("SVE.UMOV", id), id))));
  }

  private InstructionTestGroup instructionGroup(String name,
                                                List<IssTestUtils.TestCase> testCases) {
    return new InstructionTestGroup(name, testCases);
  }

  private List<IssTestUtils.TestCase> predicatedBinaryInstrTests(String instruction,
                                                                 String... elemSuffixes) {
    var generators = Arrays.stream(elemSuffixes)
        .map(elem -> (Function<Integer, IssTestUtils.TestCase>) (id -> {
          var builder = getBuilder("SVE.PRED." + instruction.toUpperCase() + "." + elem, id);
          return createPredicatedBinaryInstrTest(builder, instruction, elem, id);
        }))
        .toList();
    return buildTestsWith(generators);
  }

  private List<IssTestUtils.TestCase> unpredicatedBinaryInstrTests(String instruction,
                                                                   String... elemSuffixes) {
    var generators = Arrays.stream(elemSuffixes)
        .map(elem -> (Function<Integer, IssTestUtils.TestCase>) (id -> {
          var builder = getBuilder("SVE.UNPRED." + instruction.toUpperCase() + "." + elem, id);
          return createUnpredicatedBinaryInstrTest(builder, instruction, elem, id);
        }))
        .toList();
    return buildTestsWith(generators);
  }

  private List<IssTestUtils.TestCase> reductionInstrTests(String instruction,
                                                          boolean extendedAddReduction,
                                                          String... elemSuffixes) {
    List<Function<Integer, IssTestUtils.TestCase>> generators = Arrays.stream(elemSuffixes)
        .map(elem -> (Function<Integer, IssTestUtils.TestCase>) (id -> {
          var builder = getBuilder("SVE.RED." + instruction.toUpperCase() + "." + elem, id);
          return createReductionInstrTest(builder, instruction, elem, extendedAddReduction, id);
        }))
        .toList();
    return buildTestsWith(generators);
  }
}
