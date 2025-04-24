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

package vadl.lcb.passes.llvmLowering.strategies.nodeLowering;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmCondCode;
import vadl.types.BuiltInTable;
import vadl.viam.ViamError;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * Replacement strategy for nodes.
 */
public class LcbWriteRegNodeReplacement
    implements GraphVisitor.NodeApplier<WriteRegTensorNode, WriteRegTensorNode> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public LcbWriteRegNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public WriteRegTensorNode visit(WriteRegTensorNode writeRegNode) {
    if (writeRegNode.hasAddress()) {
      visitApplicable(writeRegNode.address());
    }

    visitApplicable(writeRegNode.value());

    if (writeRegNode.isPcAccess()) {
      if (writeRegNode.value() instanceof BuiltInCall builtin && Set.of(
          BuiltInTable.ADD,
          BuiltInTable.ADDS,
          BuiltInTable.SUB
      ).contains(builtin.builtIn())) {
        // We need four parameters to replace a memory write by `LlvmBrCcSD`.
        // 1. the conditional code (SETEQ, ...)
        // 2. the first operand of the comparison
        // 3. the second operand of the comparison
        // 4. the immediate offset

        var conditional = (BuiltInCall) writeRegNode.condition();
        var condCond = LlvmCondCode.from(conditional.builtIn());
        if (condCond == null) {
          throw new ViamError("CondCode must be not null");
        }

        visitApplicable(conditional.arguments().get(0));
        visitApplicable(conditional.arguments().get(1));

        var first = conditional.arguments().get(0);
        var second = conditional.arguments().get(1);
        var immOffset =
            builtin.arguments().stream().filter(x -> x instanceof FieldAccessRefNode)
                .findFirst();

        if (immOffset.isEmpty()) {
          throw new ViamError("Immediate Offset is missing");
        }

        writeRegNode.value().replaceAndDelete(new LlvmBrCcSD(
            condCond,
            first,
            second,
            immOffset.get()
        ));
      }
    }

    return writeRegNode;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof WriteRegTensorNode writeRegTensorNode
        && writeRegTensorNode.regTensor().isSingleRegister();
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
