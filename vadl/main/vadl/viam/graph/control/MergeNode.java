// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

import java.util.List;
import java.util.stream.Collectors;
import vadl.javaannotations.viam.Input;
import vadl.utils.GraphUtils;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * The MergeNode depends on a list of branches that merge control flow again.
 */
public class MergeNode extends AbstractBeginNode {

  @Input
  NodeList<BranchEndNode> branchEnds;

  public MergeNode(NodeList<BranchEndNode> branchEnds, ControlNode next) {
    super(next);
    this.branchEnds = branchEnds;
  }

  public MergeNode(NodeList<BranchEndNode> branchEnds) {
    this.branchEnds = branchEnds;
  }

  /**
   * Use this only if you know that the merge node corresponds to an {@link IfNode}.
   */
  public BranchEndNode trueBranchEnd() {
    ensure(branchEnds.size() == 2,
        "The merge node does not correspond to an if node, as it has not exactly two branch ends.");
    return branchEnds.getFirst();
  }

  /**
   * Use this only if you know that the merge node corresponds to an {@link IfNode}.
   */
  public BranchEndNode falseBranchEnd() {
    ensure(branchEnds.size() == 2,
        "The merge node does not correspond to an if node, as it has not exactly two branch ends.");
    return branchEnds.getLast();
  }

  /**
   * Returns the control split that corresponds to this merge. This is the
   * {@link ControlSplitNode} whose branches terminate at the {@link BranchEndNode}s
   * consumed by this merge.
   */
  public ControlSplitNode controlSplit() {
    // We can use any branch end; take the first one.
    ControlNode curr = trueBranchEnd();
    while (!(curr instanceof ControlSplitNode controlSplitNode)) {
      ensure(curr != null, "Reached node with no predecessor, which should be impossible.");
      curr = GraphUtils.predecessorSkippingMerges(curr);
    }
    return controlSplitNode;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(branchEnds);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    branchEnds = rewriteNodeList(branchEnds, visitor, BranchEndNode.class);
  }

  @Override
  public String toString() {
    var ids = branchEnds.stream()
        .map(e -> "%s".formatted(e.id))
        .collect(Collectors.joining(", "));
    return "%s(%s)".formatted(super.toString(), ids);
  }

  @Override
  public Node copy() {
    return new MergeNode(
        new NodeList<>(this.branchEnds.stream().map(x -> (BranchEndNode) x.copy()).toList()),
        (ControlNode) next().copy());
  }

  @Override
  public Node shallowCopy() {
    return new MergeNode(branchEnds, (ControlNode) next());
  }
}
