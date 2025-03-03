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

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import javax.annotation.Nullable;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.types.DataType;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Replacement strategy for nodes.
 */
public class LcbConstantNodeReplacement
    implements GraphVisitor.NodeApplier<ConstantNode, ConstantNode> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public LcbConstantNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  /**
   * This method looks at the usages of the given {@code node} and updates the type
   * based on the type of the usage. This is necessary because TableGen cannot cast implicitly.
   */
  public static ConstantNode updateConstant(ConstantNode node) {
    var types = node.usages()
        .filter(x -> x instanceof ExpressionNode)
        .map(x -> {
          var y = (ExpressionNode) x;
          // Cast to BitsType when SIntType
          return y.type();
        })
        .filter(x -> x instanceof DataType)
        .map(x -> (DataType) x)
        .sorted(Comparator.comparingInt(DataType::bitWidth))
        .toList();

    var distinctTypes = new HashSet<>(types);

    if (distinctTypes.size() > 1) {
      DeferredDiagnosticStore.add(
          Diagnostic.warning("Constant must be upcasted but it has multiple candidates. "
                  + "The compiler generator considered only the first type as upcast.",
              node.sourceLocation()).build());
    } else if (distinctTypes.isEmpty()) {
      DeferredDiagnosticStore.add(
          Diagnostic.warning("Constant must be upcasted but it has no candidates.",
              node.sourceLocation()).build());
      return node;
    }

    var type = types.stream().findFirst().get();
    node.setType(type);
    node.constant().setType(type);
    return node;
  }

  @Nullable
  @Override
  public ConstantNode visit(ConstantNode node) {
    return updateConstant(node);
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof ConstantNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
