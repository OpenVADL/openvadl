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

import java.util.List;
import java.util.stream.Collectors;
import vadl.javaannotations.viam.Input;
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

  public BranchEndNode trueBranchEnd() {
    return branchEnds.get(0);
  }

  public BranchEndNode falseBranchEnd() {
    return branchEnds.get(branchEnds.size() - 1);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(branchEnds);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    branchEnds = branchEnds.stream()
        .map(e -> visitor.apply(this, e, BranchEndNode.class))
        .collect(Collectors.toCollection(NodeList::new));
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
