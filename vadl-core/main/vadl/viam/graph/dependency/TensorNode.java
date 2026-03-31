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

package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * An expression node representing the {@code forall i in <range> tensor <body>} language feature.
 * It performs the expression for all {@code i} in parallel and concatenates it to a scalar value.
 * The index is referenced via the {@link ForIdxNode} index field.
 *
 * @see ForIdxNode
 * @see FoldNode
 * @see vadl.viam.graph.control.ForallNode
 */
public class TensorNode extends ExpressionNode {

  @Input
  private ForIdxNode idx;
  @Input
  private ExpressionNode body;

  /**
   * Construct the index node.
   *
   * @param type of the tensor expression result.
   * @param idx  of the tensor forall expression.
   * @param body of the tensor that is evaluated for each index and concatenated.
   */
  public TensorNode(Type type, ForIdxNode idx, ExpressionNode body) {
    super(type);
    this.idx = idx;
    this.body = body;
  }

  public ForIdxNode idx() {
    return idx;
  }

  public ExpressionNode body() {
    return body;
  }

  @Override
  public DataType type() {
    return (DataType) super.type();
  }

  @Override
  public ExpressionNode copy() {
    return new TensorNode(type(), idx.copy(), body.copy());
  }

  @Override
  public Node shallowCopy() {
    return new TensorNode(type(), idx, body);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(idx);
    collection.add(body);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    idx = visitor.apply(this, idx, ForIdxNode.class);
    body = visitor.apply(this, body, ExpressionNode.class);
  }
}
