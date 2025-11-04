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
import javax.annotation.Nonnull;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * The end node to the corresponding {@link ForallNode}.
 * As the {@link ForallNode} is not diverging control flow, this is not an end node,
 * but just a directional node.
 */
public class ForallEndNode extends DirectionalNode {
  @Input
  private NodeList<SideEffectNode> sideEffects;

  public ForallEndNode(@Nonnull ControlNode next, NodeList<SideEffectNode> sideEffects) {
    super(next);
    this.sideEffects = sideEffects;
  }

  public ForallEndNode(NodeList<SideEffectNode> sideEffects) {
    this.sideEffects = sideEffects;
  }

  public NodeList<SideEffectNode> sideEffects() {
    return sideEffects;
  }

  @Override
  public Node copy() {
    return new ForallEndNode(next().copy(ControlNode.class), sideEffects.copy());
  }

  @Override
  public Node shallowCopy() {
    return new ForallEndNode(next(), sideEffects);
  }


  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(sideEffects);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    sideEffects = sideEffects.stream()
        .map(e -> visitor.apply(this, e, SideEffectNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }

}
