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
import javax.annotation.Nullable;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.TruncateNode;

/**
 * Replacement strategy for nodes.
 */
public class LcbTruncateNodeReplacement
    implements GraphVisitor.NodeApplier<TruncateNode, TruncateNode> {
  private final List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer;

  public LcbTruncateNodeReplacement(
      List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacer) {
    this.replacer = replacer;
  }

  @Nullable
  @Override
  public TruncateNode visit(TruncateNode truncateNode) {
    // Remove all nodes
    for (var usage : truncateNode.usages().toList()) {
      usage.replaceInput(truncateNode, truncateNode.value());
    }
    
    visitApplicable(truncateNode.value());
    return truncateNode;
  }

  @Override
  public boolean acceptable(Node node) {
    return node instanceof TruncateNode;
  }

  @Override
  public List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> recursiveHooks() {
    return replacer;
  }
}
