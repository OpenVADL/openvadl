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

package vadl.viam.graph.control;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Successor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.passes.CfgTraverser;

/**
 * Represents a control flow node that causes diverging execution.
 */
public abstract class ControlSplitNode extends ControlNode {

  @Successor
  private NodeList<BeginNode> branches;

  @LazyInit
  private MergeNode mergeNode;

  ControlSplitNode(NodeList<BeginNode> branches) {
    this.branches = branches;
  }

  public NodeList<BeginNode> branches() {
    return branches;
  }

  @Nullable
  @Override
  public DirectionalNode predecessor() {
    var predecessor = super.predecessor();
    ensure(predecessor == null || predecessor instanceof DirectionalNode,
        "The predecessor of a control split must be a directional node, but was: %s",
        predecessor);
    return (DirectionalNode) predecessor;
  }

  @Override
  protected void collectSuccessors(List<Node> collection) {
    super.collectSuccessors(collection);
    collection.addAll(branches);
  }

  @Override
  protected void applyOnSuccessorsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnSuccessorsUnsafe(visitor);
    branches = branches.stream().map(e ->
            visitor.apply(this, e, BeginNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }

  public void clearPredecessor() {
    this.setPredecessor(null);
  }

  /**
   * Get the {@code mergeNode}.
   */
  public MergeNode mergeNode() {
    if (mergeNode == null) {
      var y = new CfgTraverser() {

      };
      var endNode = y.traverseBranch(this.branches.get(0));
      mergeNode = endNode.usages().filter(x -> x instanceof MergeNode)
          .map(x -> (MergeNode) x)
          .findFirst().get();
    }

    return mergeNode;
  }
}
