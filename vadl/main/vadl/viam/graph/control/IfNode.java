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
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.passes.CfgTraverser;


/**
 * IfNode is a class that represents an if statement in a control flow graph.
 * It extends the ControlSplitNode class, which is a control flow node that
 * causes diverging execution.
 *
 * <p>The IfNode class has the following fields:
 * <li>{@code condition}: An ExpressionNode object that represents the condition of
 * the if statement.</li>
 * <li>{@code trueBranch}: A Node object that represents the true branch of the if statement.</li>
 * <li>{@code falseBranch}: A Node object that represents the false branch of the if statement.</li>
 *
 * <p>This class uses the following annotations:
 * <li>{@code @Input}: Marks the condition field as an input field, pointing to another node.</li>
 * <li>{@code @Successor}: Marks the trueBranch and falseBranch fields as successor node properties
 * of the if statement.</li>
 */
public class IfNode extends ControlSplitNode {

  @Input
  private ExpressionNode condition;

  /**
   * The constructor to instantiate a IfNode.
   */
  public IfNode(ExpressionNode condition, BeginNode trueBranch, BeginNode falseBranch) {
    super(new NodeList<>(trueBranch, falseBranch));
    this.condition = condition;
  }

  public ExpressionNode condition() {
    return condition;
  }

  public BeginNode trueBranch() {
    return branches().get(0);
  }

  public BeginNode falseBranch() {
    return branches().get(1);
  }

  /**
   * This finds the merge node that corresponds to this if.
   * Only use it if necessary, as it has to traverse the control flow
   * until it reaches the end of one branch.
   */
  public MergeNode findCorrespondingMergeNode() {
    var endFalseBranch = new CfgTraverser() {
    }.traverseBranch(falseBranch());
    var mergeNode = endFalseBranch.usages().findFirst();
    ensure(mergeNode.isPresent() && mergeNode.get() instanceof MergeNode,
        "False branch end node is not a merge node... corrupted graph.");
    return (MergeNode) mergeNode.get();
  }

  @Override
  public Node copy() {
    return new IfNode((ExpressionNode) condition.copy(), (BeginNode) trueBranch().copy(),
        (BeginNode) falseBranch().copy());
  }

  @Override
  public Node shallowCopy() {
    return new IfNode(condition, trueBranch(), falseBranch());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "%s(t: %s, f: %s)".formatted(super.toString(), trueBranch().id, falseBranch().id);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(condition);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    condition = visitor.apply(this, condition, ExpressionNode.class);
  }

}
