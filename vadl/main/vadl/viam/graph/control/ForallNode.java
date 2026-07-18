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

import java.util.function.Consumer;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ForIdxNode;

/**
 * Represents a {@code forall i in <range> do{...}} statement.
 * It is followed by a {@link BranchBeginNode} and ends with an {@link AbstractEndNode}
 * which is consumed by a {@link ForallEndNode}.
 *
 * <p>Its index is represented as a dependency to a {@link ForIdxNode}.
 *
 * <p>The control flow looks like this:
 * <pre>
 * {@code
 * ... -> ForallNode -> BranchBeginNode -> ... -> BranchEndNode(with side-effects) -> ForallEndNode
 * }
 * </pre>
 *
 * @see ForIdxNode
 * @see vadl.viam.graph.dependency.TensorNode
 * @see vadl.viam.graph.dependency.FoldNode
 * @see <a href="https://github.com/OpenVADL/openvadl/pull/566">Github PR #566</a>
 */
public class ForallNode extends ControlSplitNode {

  @Input
  private ForIdxNode idx;

  public ForallNode(ForIdxNode idx, BranchBeginNode beginNode) {
    super(new NodeList<>(beginNode));
    this.idx = idx;
  }

  public ForIdxNode idx() {
    return idx;
  }

  public BranchBeginNode beginNode() {
    return branches().getFirst();
  }

  public boolean isEmpty() {
    return beginNode().next() instanceof AbstractEndNode;
  }

  @Override
  public Node copy() {
    return new ForallNode(idx.copy(), beginNode());
  }

  @Override
  public Node shallowCopy() {
    return new ForallNode(idx, beginNode());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void forEachInput(Consumer<Node> consumer) {
    super.forEachInput(consumer);
    consumer.accept(idx);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    idx = visitor.apply(this, idx, ForIdxNode.class);
  }
}
