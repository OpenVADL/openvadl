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

package vadl.viam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static vadl.utils.GraphUtils.getSingleLeafNode;
import static vadl.utils.GraphUtils.getSingleNode;

import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
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
        arguments("reg_invalidFormat", "Invalid Format")
    );
  }

  @ParameterizedTest(name = "{index} {0}")
  @MethodSource("invalidRegisterTestSources")
  public void invalidRegister(String testSource, @Nullable String failureMessage) {
    runAndAssumeFailure(testSource, failureMessage);
  }

  @TestFactory
  public Stream<DynamicTest> testRegfile() {
    var spec = runAndGetViamSpecification("unit/register/valid_regfile.vadl");

    return Stream.of(
        dynamicTest("Test::X", () -> {
          var x = (RegisterFile) TestUtils.findResourceByName("Test::X", spec);
          assertTrue(x.hasAddress());
          assertEquals(Type.bits(5), x.addressType());
          assertEquals(Type.bits(32), x.resultType());

          // FIXME: Renable once we parse annotations
          //assertEquals(0, x.constraints().length);
        }),
        dynamicTest("Test::Y", () -> {
          var y = (RegisterFile) TestUtils.findResourceByName("Test::Y", spec);
          // FIXME: Renable once we parse annotations
          // var constraints = y.constraints();
          // assertEquals(1, constraints.length);
          // var constraint = constraints[0];
          // assertEquals(2, constraint.indices().getFirst().integer().intValue());
          // assertEquals(0, constraint.value().integer().intValue());
        })
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
  //@TestFactory
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

  private DynamicTest testRegister(Register reg, Type resType) {
    return dynamicTest(reg.simpleName(), () -> {
      assertFalse(false);
      assertEquals(resType, reg.resultType());
    });
  }


  // FIXME: @ffreitag part of https://ea.complang.tuwien.ac.at/vadl/open-vadl/issues/377
  // @TestFactory
  public Stream<DynamicTest> testPcReg() {
    return Stream.of(
        testPc("valid_pc_normal.vadl", "PcTest::PC", "PcTest::PC", null),
        testPc("valid_pc_alias_reg.vadl", "PcTest::PC", "PcTest::A", null),
        testPc("valid_pc_alias_regfile.vadl", "PcTest::PC", "PcTest::X", 31),
        testPc("valid_pc_current.vadl", "PcTest::PC", "PcTest::PC", null),
        testPc("valid_pc_next.vadl", "PcTest::PC", "PcTest::PC", null),
        testPc("valid_pc_next_next.vadl", "PcTest::PC", "PcTest::PC", null)
    );

  }

  private DynamicTest testPc(String fileName, String counterName, String resourceName,
                             @Nullable Integer index) {
    return dynamicTest(fileName, () -> {
      var spec = runAndGetViamSpecification("unit/register/" + fileName);
      var resource = TestUtils.findDefinitionByNameIn(resourceName, spec, Resource.class);
      var counter = TestUtils.findDefinitionByNameIn(counterName, spec, Counter.class);

      var readInstr = TestUtils.findDefinitionByNameIn("PcTest::READ_PC", spec, Instruction.class);
      var writeInstr =
          TestUtils.findDefinitionByNameIn("PcTest::WRITE_PC", spec, Instruction.class);

      Assertions.assertEquals(resource, counter.registerTensor());
      if (index != null) {
        assertInstanceOf(Counter.class, counter);
        assertEquals(index, counter.indices().getFirst().intValue());
      }

      if (resource instanceof Register) {
        var readReg = getSingleNode(readInstr.behavior(), ReadRegNode.class);
        Assertions.assertEquals(resource, readReg.register());
        var writeReg = getSingleNode(writeInstr.behavior(), WriteRegNode.class);
        Assertions.assertEquals(resource, writeReg.register());
      } else {
        var readReg = getSingleNode(readInstr.behavior(), ReadRegFileNode.class);
        Assertions.assertEquals(resource, readReg.registerFile());
        var readAddrConst = getSingleLeafNode(readReg.address(), ConstantNode.class);
        Assertions.assertEquals(counter.indices().getFirst(), readAddrConst.constant().asVal());
        var writeReg = getSingleNode(writeInstr.behavior(), WriteRegFileNode.class);
        Assertions.assertEquals(resource, writeReg.registerFile());
        var writeAddrConst = getSingleLeafNode(writeReg.address(), ConstantNode.class);
        Assertions.assertEquals(counter.indices().getFirst(), (writeAddrConst.constant().asVal()));
      }
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
