// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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
import java.util.stream.Collectors;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.Operation;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * An expression node representing the {@code forall i in {<operations>} then <body>}
 * expression.
 */
public class OperationForAllNode extends ExpressionNode {

  @Input
  private NodeList<Index> indices;

  @Input
  private ExpressionNode body;

  /**
   * The constructor.
   *
   * @param type    type of the forall then node.
   * @param indices the bound variables.
   * @param body    the expression body.
   */
  public OperationForAllNode(Type type, List<Index> indices, ExpressionNode body) {
    super(type);
    this.indices = new NodeList<>(indices);
    this.body = body;
  }

  public List<Index> indices() {
    return indices;
  }

  public ExpressionNode body() {
    return body;
  }

  @Override
  public ExpressionNode copy() {
    return new OperationForAllNode(type(),
        indices.stream().map(Index::copy).map(Index.class::cast).toList(),
        body.copy());
  }

  @Override
  public Node shallowCopy() {
    return new OperationForAllNode(type(), indices, body);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(indices);
    collection.add(body);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    indices = indices.stream().map((e) -> visitor.apply(this, e, Index.class))
        .collect(Collectors.toCollection(NodeList::new));
    body = visitor.apply(this, body, ExpressionNode.class);
  }

  /**
   * An expression node representing an index of the forall-then expression. That is, a
   * bound variable restricted to some operation set.
   */
  public static class Index extends ExpressionNode {

    @DataValue
    private final List<Operation> operations;

    /**
     * The constructor.
     *
     * @param type       type of the expression.
     * @param operations operation restrictions.
     */
    public Index(Type type, List<Operation> operations) {
      super(type);
      this.operations = operations;
    }

    public List<Operation> operations() {
      return operations;
    }

    @Override
    public ExpressionNode copy() {
      return new Index(type(), operations);
    }

    @Override
    public Node shallowCopy() {
      return new Index(type(), operations);
    }

    @Override
    protected void collectData(List<Object> collection) {
      super.collectData(collection);
      collection.add(operations);
    }
  }
}
