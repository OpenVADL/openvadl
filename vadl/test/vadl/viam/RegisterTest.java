package vadl.viam;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static vadl.utils.GraphUtils.getSingleLeafNode;
import static vadl.utils.GraphUtils.getSingleNode;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.AbstractTest;
import vadl.TestUtils;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.ConstantValueMatcher;


public class RegisterTest extends AbstractTest {

  private static Stream<Arguments> invalidRegisterTestSources() {
    return AbstractTest.getTestSourceArgsForParameterizedTest("unit/register/invalid_",
        arguments("reg_invalidFormat",
            "Format field must only contain proper slices without any unused gaps.")
    );
  }

  // FIXME: @ffreitag part of https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/377
  // @ParameterizedTest(name = "{index} {0}")
  @MethodSource("invalidRegisterTestSources")
  public void invalidRegister(String testSource, @Nullable String failureMessage) {
    runAndAssumeFailure(testSource, failureMessage);
  }

  // FIXME: @ffreitag part of https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/377
  // @TestFactory
  public Stream<DynamicTest> testRegfile() {
    var spec = runAndGetViamSpecification("unit/register/valid_regfile.vadl");

    return Stream.of(
        dynamicTest("Test::X", () -> {
          var x = (RegisterFile) TestUtils.findResourceByName("Test::X", spec);
          assertTrue(x.hasAddress());
          assertEquals(Type.bits(5), x.addressType());
          assertEquals(Type.bits(32), x.resultType());
          assertEquals(0, x.constraints().length);
        }),
        dynamicTest("Test::Y", () -> {
          var y = (RegisterFile) TestUtils.findResourceByName("Test::Y", spec);
          var constraints = y.constraints();
          assertEquals(1, constraints.length);
          var constraint = constraints[0];
          assertEquals(2, constraint.address().integer().intValue());
          assertEquals(0, constraint.value().integer().intValue());
        })
    );
  }

  // FIXME: @ffreitag part of https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/377
  // @TestFactory
  public Stream<DynamicTest> testReg() {
    var spec = runAndGetViamSpecification("unit/register/valid_reg_definition.vadl");
    var testIsa = TestUtils.findDefinitionByNameIn("Test", spec, InstructionSetArchitecture.class);

    var parAcc = Register.AccessKind.PARTIAL;
    var fulAcc = Register.AccessKind.FULL;

    var x = (Register) TestUtils.findResourceByName("Test::X", spec);
    {
      testRegister(x, Type.bits(32), Set.of(), null, null, parAcc, parAcc);
    }

    var PP = (Register) TestUtils.findResourceByName("Test::PP", spec);
    var PP_N = (Register) TestUtils.findResourceByName("Test::PP_N", spec);
    var PP_N_H = (Register) TestUtils.findResourceByName("Test::PP_N_H", spec);
    var PP_N_L = (Register) TestUtils.findResourceByName("Test::PP_N_L", spec);
    var PP_P = (Register) TestUtils.findResourceByName("Test::PP_P", spec);
    var PF = (Register) TestUtils.findResourceByName("Test::PF", spec);
    var PF_N = (Register) TestUtils.findResourceByName("Test::PF_N", spec);
    var PF_N_H = (Register) TestUtils.findResourceByName("Test::PF_N_H", spec);
    var PF_N_L = (Register) TestUtils.findResourceByName("Test::PF_N_L", spec);
    var PF_P = (Register) TestUtils.findResourceByName("Test::PF_P", spec);
    var FP = (Register) TestUtils.findResourceByName("Test::FP", spec);
    var FP_N = (Register) TestUtils.findResourceByName("Test::FP_N", spec);
    var FP_N_H = (Register) TestUtils.findResourceByName("Test::FP_N_H", spec);
    var FP_N_L = (Register) TestUtils.findResourceByName("Test::FP_N_L", spec);
    var FP_P = (Register) TestUtils.findResourceByName("Test::FP_P", spec);
    var FF = (Register) TestUtils.findResourceByName("Test::FF", spec);

    var outer = TestUtils.findFormatByName("Test::OUTER", spec);
    var inner = TestUtils.findFormatByName("INNER", spec);

    var outer_t = Type.bits(18);
    var inner_t = Type.bits(10);

    var allRegs = Set.of(
        x,
        PP,
        PP_N,
        PP_N_H,
        PP_N_L,
        PP_P,
        PF,
        PF_N,
        PF_N_H,
        PF_N_L,
        PF_P,
        FP,
        FP_N,
        FP_N_H,
        FP_N_L,
        FP_P,
        FF
    );

    return Stream.of(
        testRegister(PP, outer_t, Set.of(PP_N, PP_P), null, outer, parAcc, parAcc),
        testRegister(PP_P, Type.bits(8), Set.of(), PP, null, parAcc, parAcc),
        testRegister(PP_N, inner_t, Set.of(PP_N_H, PP_N_L), PP, inner, parAcc, parAcc),
        testRegister(PP_N_H, Type.bits(6), Set.of(), PP_N, null, parAcc, parAcc),
        testRegister(PP_N_L, Type.bits(4), Set.of(), PP_N, null, parAcc, parAcc),

        testRegister(PF, outer_t, Set.of(PF_N, PF_P), null, outer, parAcc, fulAcc),
        testRegister(PF_P, Type.bits(8), Set.of(), PF, null, parAcc, fulAcc),
        testRegister(PF_N, inner_t, Set.of(PF_N_H, PF_N_L), PF, inner, parAcc, fulAcc),
        testRegister(PF_N_H, Type.bits(6), Set.of(), PF_N, null, parAcc, fulAcc),
        testRegister(PF_N_L, Type.bits(4), Set.of(), PF_N, null, parAcc, fulAcc),

        testRegister(FP, outer_t, Set.of(FP_N, FP_P), null, outer, fulAcc, parAcc),
        testRegister(FP_P, Type.bits(8), Set.of(), FP, null, fulAcc, parAcc),
        testRegister(FP_N, inner_t, Set.of(FP_N_H, FP_N_L), FP, inner, fulAcc, parAcc),
        testRegister(FP_N_H, Type.bits(6), Set.of(), FP_N, null, fulAcc, parAcc),
        testRegister(FP_N_L, Type.bits(4), Set.of(), FP_N, null, fulAcc, parAcc),

        testRegister(FF, outer_t, Set.of(), null, outer, fulAcc, fulAcc),
        dynamicTest("AllRegs",
            () -> MatcherAssert.assertThat(testIsa.ownRegisters(), containsInAnyOrder(allRegs.toArray()))
        )
    );
  }

  // FIXME: @ffreitag part of https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/377
  // @TestFactory
  Stream<DynamicTest> testRegRead() {
    var spec = runAndGetViamSpecification("unit/register/valid_reg_read.vadl");
    var a = (Register) TestUtils.findResourceByName("Test::A", spec);
    var b = (Register) TestUtils.findResourceByName("Test::B", spec);
    var b_one = (Register) TestUtils.findResourceByName("Test::B_ONE", spec);
    var c = (Register) TestUtils.findResourceByName("Test::C", spec);
    var d = (RegisterFile) TestUtils.findResourceByName("Test::D", spec);

    return Stream.of(

        dynamicTest("Test::FIRST", () -> {
          var first = TestUtils.findDefinitionByNameIn("Test::FIRST", spec, Instruction.class);
          var behavior = first.behavior();
          behavior.verify();
          var depNodes = behavior.getNodes(DependencyNode.class).toList();
          Assertions.assertEquals(2, depNodes.size());

          var readNodeOpt = first.behavior().getNodes(ReadRegNode.class).findFirst();
          Assertions.assertTrue(readNodeOpt.isPresent());
          var readNode = readNodeOpt.get();

          Assertions.assertFalse(readNode.hasAddress());
          Assertions.assertEquals(b, readNode.register());

          var writeNodeOpt = first.behavior().getNodes(WriteRegNode.class).findFirst();
          Assertions.assertTrue(writeNodeOpt.isPresent());
          var writeNode = writeNodeOpt.get();

          Assertions.assertEquals(readNode, writeNode.value());
          Assertions.assertFalse(writeNode.hasAddress());
          Assertions.assertEquals(a, writeNode.register());
        }),

        dynamicTest("Test::SECOND", () -> {
          var second = TestUtils.findDefinitionByNameIn("Test::SECOND", spec, Instruction.class);
          var behavior = second.behavior();
          behavior.verify();
          var depNodes = behavior.getNodes(DependencyNode.class).toList();
          Assertions.assertEquals(2, depNodes.size());

          var readNodeOpt = second.behavior().getNodes(ReadRegNode.class).findFirst();
          Assertions.assertTrue(readNodeOpt.isPresent());
          var readNode = readNodeOpt.get();

          Assertions.assertFalse(readNode.hasAddress());
          Assertions.assertEquals(b_one, readNode.register());

          var writeNodeOpt = second.behavior().getNodes(WriteRegNode.class).findFirst();
          Assertions.assertTrue(writeNodeOpt.isPresent());
          var writeNode = writeNodeOpt.get();

          Assertions.assertEquals(readNode, writeNode.value());
          Assertions.assertFalse(writeNode.hasAddress());
          Assertions.assertEquals(a, writeNode.register());
        }),

        dynamicTest("Test::THIRD", () -> {
          var third = TestUtils.findDefinitionByNameIn("Test::THIRD", spec, Instruction.class);
          var behavior = third.behavior();
          behavior.verify();
          var depNodes = behavior.getNodes(DependencyNode.class).toList();
          Assertions.assertEquals(3, depNodes.size());

          var readNodeOpt = third.behavior().getNodes(ReadRegNode.class).findFirst();
          Assertions.assertTrue(readNodeOpt.isPresent());
          var readNode = readNodeOpt.get();

          Assertions.assertFalse(readNode.hasAddress());
          Assertions.assertEquals(c, readNode.register());

          var funcCallNode = behavior.getNodes(FuncCallNode.class).findFirst().get();
          Assertions.assertEquals(1, funcCallNode.arguments().size());
          Assertions.assertEquals(readNode, funcCallNode.arguments().get(0));
          Assertions.assertEquals(Objects.requireNonNull(c.refFormat()).fields()[0].extractFunction(),
              funcCallNode.function());

          var writeNodeOpt = third.behavior().getNodes(WriteRegNode.class).findFirst();
          Assertions.assertTrue(writeNodeOpt.isPresent());
          var writeNode = writeNodeOpt.get();

          Assertions.assertEquals(funcCallNode, writeNode.value());
          Assertions.assertFalse(writeNode.hasAddress());
          Assertions.assertEquals(a, writeNode.register());
        }),

        dynamicTest("Test::FOURTH", () -> {
          var fourth = TestUtils.findDefinitionByNameIn("Test::FOURTH", spec, Instruction.class);
          var behavior = fourth.behavior();
          behavior.verify();
          var depNodes = behavior.getNodes(DependencyNode.class).toList();
          Assertions.assertEquals(3, depNodes.size());

          var readNode = behavior.getNodes(ReadRegNode.class).findFirst().get();
          Assertions.assertEquals(b_one, readNode.register());

          var readFileNode = behavior.getNodes(ReadRegFileNode.class).findFirst().get();
          Assertions.assertEquals(readNode, readFileNode.address());
          Assertions.assertEquals(d, readFileNode.registerFile());

          var writeNode = behavior.getNodes(WriteRegNode.class).findFirst().get();
          Assertions.assertEquals(readFileNode, writeNode.value());
          Assertions.assertEquals(a, writeNode.register());
        })
    );
  }

  // FIXME: @ffreitag part of https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/377
  // @TestFactory
  Stream<DynamicTest> testWriteReg() {
    var spec = runAndGetViamSpecification("unit/register/valid_reg_write.vadl");
    var b = (Register) TestUtils.findResourceByName("Test::B", spec);
    var b_one = (Register) TestUtils.findResourceByName("Test::B_ONE", spec);
    var d = (RegisterFile) TestUtils.findResourceByName("Test::D", spec);

    return Stream.of(

        dynamicTest("Test::FIRST", () -> {
          var instr = TestUtils.findDefinitionByNameIn("Test::FIRST", spec, Instruction.class);
          var behavior = instr.behavior();
          behavior.verify();
          var depNodes = behavior.getNodes(DependencyNode.class).toList();
          Assertions.assertEquals(2, depNodes.size());

          var readNode = behavior.getNodes(ReadRegNode.class).findFirst().get();
          var writeNode = behavior.getNodes(WriteRegNode.class).findFirst().get();
          Assertions.assertEquals(readNode, writeNode.value());
          Assertions.assertEquals(writeNode.register(), b);
        }),

        dynamicTest("Test::SECOND", () -> {
          var instr = TestUtils.findDefinitionByNameIn("Test::SECOND", spec, Instruction.class);
          var behavior = instr.behavior();
          behavior.verify();
          var depNodes = behavior.getNodes(DependencyNode.class).toList();
          Assertions.assertEquals(2, depNodes.size());

          var readNode = behavior.getNodes(ReadRegNode.class).findFirst().get();
          var writeNode = behavior.getNodes(WriteRegNode.class).findFirst().get();
          Assertions.assertEquals(readNode, writeNode.value());
          Assertions.assertEquals(writeNode.register(), b_one);
        }),

        dynamicTest("Test::FOURTH", () -> {
          var instr = TestUtils.findDefinitionByNameIn("Test::FOURTH", spec, Instruction.class);
          var behavior = instr.behavior();
          behavior.verify();
          var depNodes = behavior.getNodes(DependencyNode.class).toList();
          Assertions.assertEquals(3, depNodes.size());

          var readNodes = behavior.getNodes(ReadRegNode.class).toList();
          Assertions.assertEquals(2, readNodes.size());
          var addrReadNode =
              readNodes.stream().filter(e -> e.register() == b_one).findFirst().get();
          var writeNode = behavior.getNodes(WriteRegFileNode.class).findFirst().get();
          Assertions.assertEquals(addrReadNode, writeNode.address());
          Assertions.assertEquals(writeNode.registerFile(), d);
        })
    );
  }

  private DynamicTest testRegister(Register reg, Type resType, Set<Register> subRegs,
                                   @Nullable Register parent, @Nullable Format format,
                                   Register.AccessKind read, Register.AccessKind write) {
    return dynamicTest(reg.simpleName(), () -> {
      assertFalse(false);
      assertEquals(resType, reg.resultType());
      assertThat(subRegs, containsInAnyOrder(reg.subRegisters()));
      assertEquals(parent, reg.parent());
      assertEquals(format, reg.refFormat());
      assertEquals(read, reg.readAccess());
      assertEquals(write, reg.writeAccess());
    });
  }


  // FIXME: @ffreitag part of https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/377
  // @TestFactory
  public Stream<DynamicTest> testPcReg() {
    return Stream.of(
        testPc("valid_pc_normal.vadl", "PcTest::PC", "PcTest::PC", null,
            Counter.Kind.PROGRAM_COUNTER, Counter.Position.CURRENT),
        testPc("valid_pc_alias_reg.vadl", "PcTest::PC", "PcTest::A", null,
            Counter.Kind.PROGRAM_COUNTER, Counter.Position.CURRENT),
        testPc("valid_pc_alias_regfile.vadl", "PcTest::PC", "PcTest::X", 31,
            Counter.Kind.PROGRAM_COUNTER, Counter.Position.CURRENT),
        testPc("valid_pc_current.vadl", "PcTest::PC", "PcTest::PC", null,
            Counter.Kind.PROGRAM_COUNTER, Counter.Position.CURRENT),
        testPc("valid_pc_next.vadl", "PcTest::PC", "PcTest::PC", null,
            Counter.Kind.PROGRAM_COUNTER, Counter.Position.NEXT),
        testPc("valid_pc_next_next.vadl", "PcTest::PC", "PcTest::PC", null,
            Counter.Kind.PROGRAM_COUNTER, Counter.Position.NEXT_NEXT)
    );

  }

  private DynamicTest testPc(String fileName, String counterName, String resourceName,
                             @Nullable Integer index,
                             Counter.Kind kind,
                             Counter.Position position) {
    return dynamicTest(fileName, () -> {
      var spec = runAndGetViamSpecification("unit/register/" + fileName);
      var resource = TestUtils.findDefinitionByNameIn(resourceName, spec, Resource.class);
      var counter = TestUtils.findDefinitionByNameIn(counterName, spec, Counter.class);

      var readInstr = TestUtils.findDefinitionByNameIn("PcTest::READ_PC", spec, Instruction.class);
      var writeInstr = TestUtils.findDefinitionByNameIn("PcTest::WRITE_PC", spec, Instruction.class);

      Assertions.assertEquals(resource, counter.registerResource());
      if (index != null) {
        assertInstanceOf(Counter.RegisterFileCounter.class, counter);
        assertEquals(index, ((Counter.RegisterFileCounter) counter).index().intValue());
      }

      if (resource instanceof Register) {
        var readReg = getSingleNode(readInstr.behavior(), ReadRegNode.class);
        Assertions.assertEquals(resource, readReg.register());
        var writeReg = getSingleNode(writeInstr.behavior(), WriteRegNode.class);
        Assertions.assertEquals(resource, writeReg.register());
      } else {
        var readReg = getSingleNode(readInstr.behavior(), ReadRegFileNode.class);
        Assertions.assertEquals(resource, readReg.registerFile());
        var regFileCoutner = (Counter.RegisterFileCounter) counter;
        var readAddrConst = getSingleLeafNode(readReg.address(), ConstantNode.class);
        Assertions.assertEquals(regFileCoutner.index(), readAddrConst.constant().asVal());
        var writeReg = getSingleNode(writeInstr.behavior(), WriteRegFileNode.class);
        Assertions.assertEquals(resource, writeReg.registerFile());
        var writeAddrConst = getSingleLeafNode(writeReg.address(), ConstantNode.class);
        Assertions.assertEquals(regFileCoutner.index(), (writeAddrConst.constant().asVal()));
      }


      Assertions.assertEquals(kind, counter.kind());
      Assertions.assertEquals(position, counter.position());
    });
  }


  // FIXME: @ffreitag part of https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/377
  // @TestFactory
  public Stream<DynamicTest> testAliasRegisterFiles() {
    var spec = runAndGetViamSpecification("unit/register/valid_alias_regfile.vadl");

    return Stream.of(
        testReadAliasRegisterFile(spec, "ReadA_Hit", 12, 12),
        testReadAliasRegisterFile(spec, "ReadA_Miss", 31, 12),
        testReadAliasRegisterFile(spec, "ReadB", 31, null),

        testWriteAliasRegisterFile(spec, "WriteA_Hit", 12, 12),
        testWriteAliasRegisterFile(spec, "WriteA_Miss", 31, 12),
        testWriteAliasRegisterFile(spec, "WriteB", 31, null)
    );

  }

  private DynamicTest testWriteAliasRegisterFile(Specification spec, String instruction,
                                                 long index,
                                                 @Nullable Integer constraintIndex) {
    return dynamicTest(instruction, () -> {
      var instr = TestUtils.findDefinitionByNameIn("Test::" + instruction, spec, Instruction.class);

      if (constraintIndex != null) {
        var ifNode = getSingleNode(instr.behavior(), IfNode.class);
        var condMatcher = new BuiltInMatcher(BuiltInTable.EQU,
            (node) -> getSingleLeafNode(node, ConstantNode.class).constant().asVal().intValue()
                == index,
            (node) -> getSingleLeafNode(node, ConstantNode.class).constant().asVal().intValue()
                == constraintIndex
        );
        assertTrue(condMatcher.matches(ifNode.condition()), "was " + ifNode.condition());

        var trueBranchEnd = (BranchEndNode) ifNode.trueBranch().next();
        var falseBranchEnd = (BranchEndNode) ifNode.falseBranch().next();
        assertEquals(0, trueBranchEnd.sideEffects().size());
        assertEquals(1, falseBranchEnd.sideEffects().size());
      } else {
        Assertions.assertEquals(0, instr.behavior().getNodes(IfNode.class).count());
      }

      var x = (RegisterFile) TestUtils.findResourceByName("Test::X", spec);
      var readRegFile = getSingleNode(instr.behavior(), ReadRegFileNode.class);
      Assertions.assertEquals(x, readRegFile.registerFile());
    });
  }

  private DynamicTest testReadAliasRegisterFile(Specification spec, String instruction,
                                                long index,
                                                @Nullable Integer constraintIndex) {
    return dynamicTest(instruction, () -> {
      var instr = TestUtils.findDefinitionByNameIn("Test::" + instruction, spec, Instruction.class);

      if (constraintIndex != null) {
        var selectNode = getSingleNode(instr.behavior(), SelectNode.class);
        var condMatcher = new BuiltInMatcher(BuiltInTable.EQU,
            (node) -> getSingleLeafNode(node, ConstantNode.class).constant().asVal().intValue()
                == index,
            (node) -> getSingleLeafNode(node, ConstantNode.class).constant().asVal().intValue()
                == constraintIndex
        );
        assertTrue(condMatcher.matches(selectNode.condition()), "was " + selectNode.condition());
        var trueMatcher = new ConstantValueMatcher(Constant.Value.of(0, Type.bits(32)));
        assertTrue(trueMatcher.matches(selectNode.trueCase()));
      } else {
        Assertions.assertEquals(0, instr.behavior().getNodes(SelectNode.class).count());
      }

      var x = (RegisterFile) TestUtils.findResourceByName("Test::X", spec);
      var readRegFile = getSingleNode(instr.behavior(), WriteRegFileNode.class);
      Assertions.assertEquals(x, readRegFile.registerFile());
    });
  }

}
