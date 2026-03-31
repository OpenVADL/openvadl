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
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Function;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * An expression node representing the {@code forall i in <range> fold <func> with <body>}
 * language feature.
 * It performs the expression for all {@code i} in parallel and folds it using the combining
 * function into a scalar.
 * The index is referenced via the {@link ForIdxNode} index field and
 * the combining function using a {@link Function} definition that is either a user-defined
 * one, or a built-in one (like a wrapper around {@code +}).
 *
 * @see ForIdxNode
 * @see TensorNode
 * @see vadl.viam.graph.control.ForallNode
 */
public class FoldNode extends ExpressionNode {

  @Input
  private ForIdxNode idx;
  @Input
  private ExpressionNode body;

  @DataValue
  private Function combiner;

  /**
   * Construct the fold node.
   *
   * @param type     of the expression
   * @param idx      of the forall fold expression
   * @param body     that is evaluated for each index and folded using the combiner function
   * @param combiner the binary combiner function that folds (accumulates) the expression result
   *                 for each index value to a scalar.
   */
  public FoldNode(Type type, ForIdxNode idx, ExpressionNode body, Function combiner) {
    super(type);
    this.idx = idx;
    this.body = body;
    this.combiner = combiner;
  }

  public ForIdxNode idx() {
    return idx;
  }

  public ExpressionNode body() {
    return body;
  }

  public Function combiner() {
    return combiner;
  }

  @Override
  public DataType type() {
    return (DataType) super.type();
  }

  @Override
  public ExpressionNode copy() {
    return new FoldNode(type(), idx.copy(), body.copy(), combiner);
  }

  @Override
  public Node shallowCopy() {
    return new FoldNode(type(), idx, body, combiner);
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
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(combiner);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    idx = visitor.apply(this, idx, ForIdxNode.class);
    body = visitor.apply(this, body, ExpressionNode.class);
  }
}
