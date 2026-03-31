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

import com.google.common.collect.Streams;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Input;
import vadl.types.DataType;
import vadl.viam.Resource;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * The ReadNode class is an abstract class that extends ExpressionNode
 * and represents a node that reads a value from an address.
 * It provides a common structure and behavior for reading nodes.
 */
public abstract class ReadResourceNode extends ExpressionNode {

  @Input
  protected NodeList<ExpressionNode> indices;

  public ReadResourceNode(@Nullable ExpressionNode address, DataType type) {
    super(type);
    this.indices = address == null ? new NodeList<>() : new NodeList<>(address);
  }

  public ReadResourceNode(NodeList<ExpressionNode> indices, DataType type) {
    super(type);
    this.indices = indices;
  }

  /**
   * Get first index expression.
   *
   * @deprecated handle all indices instead ({@link #indices()}).
   */
  @Deprecated
  public ExpressionNode address() {
    ensure(indices.size() == 1, "Indices size is not 1. Check hasAddress before access.");
    return indices.getFirst();
  }

  /**
   * Check if this node has one index.
   *
   * @deprecated use {@link #indices()} instead.
   */
  @Deprecated
  public boolean hasAddress() {
    return indices.size() == 1;
  }

  @Override
  public DataType type() {
    return (DataType) super.type();
  }

  public abstract Resource resourceDefinition();

  public NodeList<ExpressionNode> indices() {
    return indices;
  }

  @Override
  public void verifyState() {
    super.verifyState();
    var resource = resourceDefinition();

    ensure(resource.indexTypes().size() >= indices().size(),
        "The resource takes %d indices but write provided %d", resource.indexTypes().size(),
        indices().size());

    Streams.forEachPair(indices.stream(), resource.indexTypes().stream(), (index, expectedType) -> {
      Objects.requireNonNull(index);
      ensure(index.type() instanceof DataType,
          "Address must be a DataValue, was %s", index.type());
      ensure(index.type().isTrivialCastTo(expectedType),
          "Address value type `%s` cannot be cast to resource's address type `%s`.",
          index.type(), expectedType);
    });
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.addAll(indices);
  }

  @Override
  public void applyOnInputsUnsafe(
      vadl.viam.graph.GraphVisitor.Applier<vadl.viam.graph.Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    indices = indices.stream().map((e) -> visitor.apply(this, e, ExpressionNode.class))
        .collect(Collectors.toCollection(NodeList::new));
  }

  /**
   * Checks whether the {@code address} of the node is constant and therefore statically known.
   */
  public boolean hasConstantAddress() {
    if (hasAddress()) {
      return address().isConstant();
    }

    return false;
  }
}
