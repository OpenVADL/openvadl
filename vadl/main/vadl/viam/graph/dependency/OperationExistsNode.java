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
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.OperationForAllNode.Index;

/**
 * An expression node representing the {@code exists i in {<operations>} then <body>}
 * expression.
 */
public class OperationExistsNode extends ExpressionNode {

  @Input
  private NodeList<Index> indices;

  @Input
  @Nullable
  private ExpressionNode body;

  /**
   * The constructor.
   *
   * @param type    type of the exists then node.
   * @param indices the bound variables.
   * @param body    the expression body.
   */
  public OperationExistsNode(Type type, List<Index> indices,
                             @CheckForNull ExpressionNode body) {
    super(type);
    this.indices = new NodeList<>(indices);
    this.body = body;
  }

  /**
   * The constructor.
   *
   * @param type  type of the exists then node.
   * @param index the bound variables.
   */
  public OperationExistsNode(Type type, Index index) {
    super(type);
    this.indices = new NodeList<>(index);
  }

  public List<Index> indices() {
    return indices;
  }

  @Nullable
  public ExpressionNode body() {
    return body;
  }

  @Override
  public ExpressionNode copy() {
    return new OperationExistsNode(type(),
        indices.stream().map(Index::copy)
            .map(Index.class::cast).toList(),
        body != null ? body.copy() : null);
  }

  @Override
  public Node shallowCopy() {
    return new OperationExistsNode(type(), indices, body);
  }

  @Override
  protected void forEachInput(Consumer<Node> consumer) {
    super.forEachInput(consumer);
    indices.forEach(consumer);
    if (this.body != null) {
      consumer.accept(body);
    }
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    indices = rewriteNodeList(indices, visitor, Index.class);
    body = visitor.applyNullable(this, body, ExpressionNode.class);
  }
}

