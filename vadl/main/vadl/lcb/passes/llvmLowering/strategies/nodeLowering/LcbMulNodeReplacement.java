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
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmSMulhSD;
import vadl.types.BuiltInTable;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.TruncateNode;

/**
 * Replacement strategy for nodes which are do multiplication and then truncate to the lower part.
 */
public class LcbMulNodeReplacement
    implements GraphVisitor.NodeApplier<BuiltInCall, BuiltInCall> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  /*
    `MUL` and `SMUL` need to be covered in the normal BuiltinReplacement.
    The reason why we are using `BuiltInTable.SMULL, BuiltInTable.SMULLS` is that the "normal"
    multiplication requires two nodes: arithmetic and slice / truncate node.
   */
  private final Set<BuiltInTable.BuiltIn> builtins =
      Set.of(BuiltInTable.SMULL, BuiltInTable.SMULLS);

  public LcbMulNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public BuiltInCall visit(BuiltInCall node) {
    for (var arg : node.arguments()) {
      visitApplicable(arg);
    }

    return node.replaceAndDelete(new LlvmSMulhSD(node.arguments(), node.type()));
  }

  @Override
  public boolean acceptable(Node node) {
    if (node instanceof BuiltInCall bc && builtins.contains(bc.builtIn())) {
      // There are two approaches:
      // (1) Cut the result
      // (2) Cut the inputs
      return
          bc.usages().allMatch(usage -> usage instanceof TruncateNode)
              || bc.arguments().stream().allMatch(arg -> arg instanceof TruncateNode);
    }

    return false;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
