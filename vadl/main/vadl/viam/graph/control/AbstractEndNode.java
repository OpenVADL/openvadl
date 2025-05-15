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
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * The AbstractEndNode represents the end of a control flow.
 * This may be a subflow (e.g. an if branch) or the main flow.
 *
 * <p>It holds a list of side effects that are required
 * to be executed before the end of this branch.</p>
 */
public abstract class AbstractEndNode extends ControlNode {
  @Input
  private NodeList<SideEffectNode> sideEffects;

  public AbstractEndNode(NodeList<SideEffectNode> sideEffects) {
    this.sideEffects = sideEffects;
  }

  public NodeList<SideEffectNode> sideEffects() {
    return sideEffects;
  }

  /**
   * Remove a side effect from this node.
   * If the side effect is present multiple times, it will only remove the first occurrence.
   */
  public void removeSideEffect(SideEffectNode sideEffect) {
    sideEffects.remove(sideEffect);
    sideEffect.removeUsage(this);
  }

  @Nonnull
  @Override
  public DirectionalNode predecessor() {
    var superNode = super.predecessor();
    ensure(superNode instanceof DirectionalNode, "Invalid predecessor %s", superNode);
    return (DirectionalNode) superNode;
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

  /**
   * Adds a side effect to the node.
   */
  public void addSideEffect(SideEffectNode sideEffectNode) {
    this.sideEffects.add(sideEffectNode);
    updateUsageOf(null, sideEffectNode);
  }
}
