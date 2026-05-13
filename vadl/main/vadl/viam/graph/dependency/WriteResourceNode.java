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

package vadl.viam.graph.dependency;

import com.google.common.collect.Streams;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Input;
import vadl.types.DataType;
import vadl.viam.Resource;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;

/**
 * Represents a write operation to some location that produces a side
 * effect.
 */
public abstract class WriteResourceNode extends SideEffectNode {

  @Input
  protected NodeList<ExpressionNode> indices;

  @Input
  protected ExpressionNode value;

  /**
   * Construct write access.
   */
  // TODO: Remove
  public WriteResourceNode(@Nullable ExpressionNode address, ExpressionNode value) {
    super(null);
    this.indices = address == null ? new NodeList<>() : new NodeList<>(address);
    this.value = value;
  }

  /**
   * Construct write access.
   */
  public WriteResourceNode(NodeList<ExpressionNode> indices, ExpressionNode value,
                           @Nullable ExpressionNode condition) {
    super(condition);
    this.indices = indices;
    this.value = value;
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

  /**
   * Checks whether the {@code address} of the node is constant and therefore statically known.
   */
  public boolean hasConstantAddress() {
    if (hasAddress()) {
      ensureNonNull(address(), "address must not be null");
      return address().isConstant();
    }

    return false;
  }

  public NodeList<ExpressionNode> indices() {
    return indices;
  }

  /**
   * Get the index of this node.
   *
   * @deprecated use {@link #indices()} instead.
   */
  @Deprecated
  public ExpressionNode address() {
    ensure(indices.size() == 1, "Indices size is not 1. Check hasAddress before access.");
    return indices.getFirst();
  }

  public ExpressionNode value() {
    return value;
  }


  /**
   * The number of bits that is getting written to the resource.
   */
  public int writeBitWidth() {
    return resourceDefinition().resultType(indices.size()).bitWidth();
  }

  @Override
  public void verifyState() {
    super.verifyState();
    var resource = resourceDefinition();

    ensure(value.type() instanceof DataType valueType
            && valueType.bitWidth() <= writeBitWidth(),
        "Mismatching resource type. Value expression's type (%s) has not the expected "
            + "width of %s.",
        value.type(), writeBitWidth());

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
    collection.add(value);
  }

  @Override
  public void applyOnInputsUnsafe(
      vadl.viam.graph.GraphVisitor.Applier<vadl.viam.graph.Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    indices = rewriteNodeList(indices, visitor, ExpressionNode.class);
    value = visitor.apply(this, value, ExpressionNode.class);
  }

  /**
   * Returns the resource affected by this write node.
   */
  public abstract Resource resourceDefinition();

}
