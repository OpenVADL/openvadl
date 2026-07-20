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

package vadl.iss.passes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.TestUtils;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.common.IssTensorAssignmentToForallPass;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.ForallEndNode;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ForIdxNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.TensorNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

public class IssTensorAssignmentToForallPassTest extends AbstractTest {

  @Test
  public void lowersWideTensorAssignmentToChunkedForallWrite() throws IOException {
    var fixture = createTensorWriteFixture("wide", 128, 16);

    runPass(fixture.specification());

    var graph = fixture.instruction().behavior();
    assertEquals(1, graph.getNodes(ForallNode.class).count());
    assertEquals(1, graph.getNodes(ForallEndNode.class).count());
    assertEquals(1, graph.getNodes(IssWriteRegNode.class)
        .filter(write -> write.windowKind() == IssWriteRegNode.WindowKind.CHUNK)
        .count());
    assertEquals(0, graph.getNodes(IssWriteRegNode.class)
        .filter(write -> write.value() instanceof TensorNode)
        .count());

    var end = graph.getNodes(InstrEndNode.class).findFirst().orElseThrow();
    assertSame(fixture.otherWrite(), end.sideEffects().getFirst());
    assertFalse(end.sideEffects().contains(fixture.tensorWrite()));
    assertTrue(fixture.tensorWrite().isDeleted());

    var chunkWrite = graph.getNodes(IssWriteRegNode.class)
        .filter(write -> write.windowKind() == IssWriteRegNode.WindowKind.CHUNK)
        .findFirst()
        .orElseThrow();
    assertEquals(8, chunkWrite.writeBitWidth());
    var value = assertInstanceOf(BuiltInCall.class, chunkWrite.value());
    var bitOffset = assertInstanceOf(BuiltInCall.class, chunkWrite.bitOffset());
    assertInstanceOf(ForIdxNode.class, value.arg(0));
    if (bitOffset.arg(0) instanceof ZeroExtendNode widenedIdx) {
      assertSame(value.arg(0), widenedIdx.value());
    } else if (bitOffset.arg(0) instanceof TruncateNode truncatedIdx) {
      assertSame(value.arg(0), truncatedIdx.value());
    } else {
      assertSame(value.arg(0), bitOffset.arg(0));
    }
  }

  @Test
  public void keepsTargetSizedTensorAssignmentIntact() throws IOException {
    var fixture = createTensorWriteFixture("target_sized", 64, 8);

    runPass(fixture.specification());

    var graph = fixture.instruction().behavior();
    assertEquals(0, graph.getNodes(ForallNode.class).count());
    assertEquals(1, graph.getNodes(IssWriteRegNode.class)
        .filter(write -> write.value() instanceof TensorNode)
        .count());
    assertFalse(fixture.tensorWrite().isDeleted());
  }

  @Test
  public void widensTensorBitOffsetArithmeticForNarrowLoopIndices() throws IOException {
    var fixture = createTensorWriteFixture("narrow_loop_idx", 256, 8, Type.bits(4), Type.bits(32));

    runPass(fixture.specification());

    var chunkWrite = fixture.instruction().behavior().getNodes(IssWriteRegNode.class)
        .filter(write -> write.windowKind() == IssWriteRegNode.WindowKind.CHUNK)
        .findFirst()
        .orElseThrow();
    var bitOffset = assertInstanceOf(BuiltInCall.class, chunkWrite.bitOffset());
    assertInstanceOf(ZeroExtendNode.class, bitOffset.arg(0));
    var elementWidth = assertInstanceOf(ConstantNode.class, bitOffset.arg(1));
    assertEquals(32, elementWidth.constant().asVal().intValue());
    var originalTensor = assertInstanceOf(TensorNode.class, fixture.tensorWrite().value());
    assertTrue(elementWidth.type().asDataType().bitWidth()
        > originalTensor.idx().type().bitWidth());
  }

  private void runPass(Specification specification) throws IOException {
    var configuration = IssConfiguration.from(getConfiguration(false));
    new IssTensorAssignmentToForallPass(configuration).execute(new PassResults(), specification);
  }

  private static Fixture createTensorWriteFixture(String name, int regBitWidth, int lanes) {
    return createTensorWriteFixture(name, regBitWidth, lanes, Type.bits(8), Type.bits(8));
  }

  private static Fixture createTensorWriteFixture(String name,
                                                  int regBitWidth,
                                                  int lanes,
                                                  DataType loopIdxType,
                                                  DataType tensorElementType) {
    var spec = TestUtils.createSpecification(name + ".specification");
    var format = TestUtils.createFormat(name + ".format", BitsType.bits(32));
    var instruction = TestUtils.createInstruction(name + ".instruction", format);
    var reg = RegisterTensor.of(TestUtils.createIdentifier(name + ".reg"), regBitWidth);
    var otherReg = RegisterTensor.of(TestUtils.createIdentifier(name + ".other_reg"), 64);

    var idx = new ForIdxNode(loopIdxType, 0, lanes - 1);
    var widenedIdx = idx.type().bitWidth() == tensorElementType.bitWidth()
        ? idx
        : new ZeroExtendNode(idx, tensorElementType);
    var tensor = new TensorNode(Type.bits(regBitWidth), idx,
        BuiltInTable.ADD.call(widenedIdx,
            Constant.Value.of(1, tensorElementType.asDataType()).toNode()));
    var tensorWrite = new IssWriteRegNode(reg, new NodeList<>(), tensor, null);
    var otherWrite = new IssWriteRegNode(otherReg, new NodeList<>(),
        Constant.Value.zero(Type.bits(64).asDataType()).toNode(), null);

    var graph = instruction.behavior();
    var end = graph.addWithInputs(new InstrEndNode(
        new NodeList<SideEffectNode>(List.of(tensorWrite, otherWrite))));
    graph.add(new StartNode(end));

    spec.add(new InstructionSetArchitecture(
        TestUtils.createIdentifier(name + ".isa"),
        spec,
        List.of(format),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        List.of(instruction),
        Collections.emptyList(),
        List.of(reg, otherReg),
        null,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        null
    ));

    return new Fixture(spec, instruction, tensorWrite, otherWrite);
  }

  private record Fixture(Specification specification,
                         vadl.viam.Instruction instruction,
                         IssWriteRegNode tensorWrite,
                         IssWriteRegNode otherWrite) {
  }
}
