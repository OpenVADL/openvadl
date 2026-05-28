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

package vadl.iss.passes.common;

import static java.util.Objects.requireNonNull;
import static vadl.iss.passes.common.opDecomposition.decomposer.ForallSubstitution.copyWithIndexSubstitution;
import static vadl.utils.StreamUtils.only;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Specification;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.BranchBeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.ForallEndNode;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.TensorNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Lowers full tensor assignments to element-wise forall statements.
 *
 * <p>Tensor expressions describe a value construction. For target-sized tensors, keeping that
 * expression intact is already efficient: helper generation can emit the tensor construction as a
 * local helper loop, and operation decomposition can still process oversized inputs below it when
 * needed.</p>
 *
 * <p>Wide tensor assignments are different. Materializing the whole tensor value would force later
 * passes to split the aggregate write into target-sized chunks. Rewriting</p>
 *
 * <pre>{@code
 * Reg := tensor_body
 * }</pre>
 *
 * <p>to</p>
 *
 * <pre>{@code
 * forall i do
 *   Reg(i) := tensor_body(i)
 * }</pre>
 *
 * <p>keeps the element computation local to the write chunk and lets the normal forall statement
 * handling decide whether helper code can keep a loop or must fall back to unrolling. End-node side
 * effects do not define a relative order, so the pass may extract one tensor write from an end node
 * that also contains other side effects.</p>
 */
public class IssTensorAssignmentToForallPass extends AbstractIssPass {

  public IssTensorAssignmentToForallPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Tensor Assignment To Forall Pass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    allInstrs(viam).forEach(instruction -> {
      while (true) {
        var candidate = instruction.behavior().getNodes(IssWriteRegNode.class)
            .filter(this::canLower)
            .findFirst();
        if (candidate.isEmpty()) {
          break;
        }
        lower(candidate.get());
      }
    });
    return null;
  }

  private boolean canLower(IssWriteRegNode write) {
    if (write.windowKind() != IssWriteRegNode.WindowKind.FULL
        || write.accessKind() != IssWriteRegNode.AccessKind.BASE
        || !(write.value() instanceof TensorNode tensor)) {
      return false;
    }

    var ends = write.usages().gather(only(AbstractEndNode.class)).toList();
    if (ends.size() != 1) {
      return false;
    }

    var end = ends.getFirst();
    var targetWidth = configuration().targetSize().width;
    return end.predecessor() != null
        && end.sideEffects().contains(write)
        && write.writeBitWidth() > targetWidth
        && tensor.body().type().asDataType().bitWidth() <= targetWidth;
  }

  private void lower(IssWriteRegNode write) {
    var tensor = (TensorNode) write.value();
    var graph = write.ensureGraph();
    var end = write.usages().gather(only(AbstractEndNode.class)).findFirst().orElseThrow();
    var predecessor = requireNonNull(end.predecessor());

    var loopIdx = tensor.idx().copy();
    var elementValue = copyWithIndexSubstitution(tensor.body(), tensor.idx(), loopIdx);
    var elementWidth = tensor.body().type().asDataType().bitWidth();
    var bitOffsetType = Type.bits(BitsType.indexWidthFor(write.writeBitWidth()));
    var bitOffset = BuiltInTable.MUL.call(
        castIndexTo(loopIdx, bitOffsetType),
        Constant.Value.of(elementWidth, bitOffsetType).toNode()
    );
    var bitWidth = Constant.Value.of(elementWidth, Type.bits(32)).toNode();

    var elementWrite = new IssWriteRegNode(
        write.regTensor(),
        write.indices().copy(),
        elementValue,
        write.staticCounterAccess(),
        write.nullableCondition() == null ? null
            : copyWithIndexSubstitution(write.nullableCondition(), tensor.idx(), loopIdx),
        write.accessKind(),
        write.writeGuardKind(),
        write.accessorName(),
        write.aliasResource(),
        new NodeList<>(write.accessorIndices().stream().map(ExpressionNode::copy).toList()),
        IssWriteRegNode.WindowKind.CHUNK,
        bitOffset,
        bitWidth
    );
    elementWrite.setSourceLocationIfNotSet(write.location());

    var branchEnd = graph.addWithInputs(new BranchEndNode(new NodeList<>(elementWrite)));
    var branchBegin = graph.add(new BranchBeginNode(branchEnd));
    var forall = graph.addWithInputs(new ForallNode(loopIdx, branchBegin));
    var forallEnd = graph.addWithInputs(new ForallEndNode(branchEnd, end));
    forallEnd.setSourceLocationIfNotSet(write.location());

    predecessor.unlinkNext();
    predecessor.setNext(forall);

    end.removeSideEffect(write);
    if (write.usageCount() == 0) {
      write.safeDelete();
    }
    forall.setSourceLocationIfNotSet(write.location());
  }

  private ExpressionNode castIndexTo(ExpressionNode index, BitsType targetType) {
    var sourceType = index.type().asDataType();
    if (sourceType.isTrivialCastTo(targetType)) {
      return index;
    }
    return sourceType.bitWidth() > targetType.bitWidth()
        ? new TruncateNode(index, targetType)
        : new ZeroExtendNode(index, targetType);
  }
}
