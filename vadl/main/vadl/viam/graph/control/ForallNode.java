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
import vadl.viam.graph.dependency.ForIdxNode;

/**
 * Represents a {@code forall i in <range> do {...}} statement.
 * The body of the forall statement starts with the next node and ends with an
 * {@link ForallEndNode}.
 * Its index is represented as a dependency to a {@link ForIdxNode}.
 *
 * @see ForIdxNode
 * @see vadl.viam.graph.dependency.TensorNode
 * @see vadl.viam.graph.dependency.FoldNode
 */
public class ForallNode extends DirectionalNode {

  @Input
  private ForIdxNode idx;

  public ForallNode(ForIdxNode idx, ControlNode next) {
    super(next);
    this.idx = idx;
  }

  public ForIdxNode idx() {
    return idx;
  }

  public boolean isEmpty() {
    return next() instanceof ForallEndNode;
  }

  @Override
  public Node copy() {
    return new ForallNode(idx.copy(), next());
  }

  @Override
  public Node shallowCopy() {
    return new ForallNode(idx, next());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(idx);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    idx = visitor.apply(this, idx, ForIdxNode.class);
  }
}
