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
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.Input;
import vadl.types.DataType;
import vadl.viam.Resource;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Represents a write operation to some location that produces a side
 * effect.
 */
public abstract class WriteResourceNode extends SideEffectNode {

  @Input
  @Nullable
  protected ExpressionNode address;

  @Input
  protected ExpressionNode value;

  public WriteResourceNode(@Nullable ExpressionNode address, ExpressionNode value) {
    this.address = address;
    this.value = value;
  }

  public void setAddress(ExpressionNode address) {
    this.address = address;
  }

  public boolean hasAddress() {
    return address != null;
  }

  /**
   * Checks whether the {@code address} of the node is constant and therefore statically knonw.
   */
  public boolean hasConstantAddress() {
    if (hasAddress()) {
      ensureNonNull(address, "address must not be null");
      return address.isConstant();
    }

    return false;
  }

  public ExpressionNode address() {
    ensureNonNull(address, "Address is not set. Check hasAddress() first.");
    return address;
  }

  public ExpressionNode value() {
    return value;
  }
  

  /**
   * The number of bits that is getting written to the resource.
   */
  protected int writeBitWidth() {
    return resourceDefinition().resultType().bitWidth();
  }

  @Override
  public void verifyState() {
    super.verifyState();
    var resource = resourceDefinition();

    ensure(value.type() instanceof DataType
            && ((DataType) value.type()).bitWidth() <= writeBitWidth(),
        "Mismatching resource type. Value expression's type (%s) cannot has not the expected "
            + "width of %s.",
        value.type(), writeBitWidth());

    ensure(resource.hasAddress() == hasAddress(),
        "Resource takes address but this node has no address node.");

    if (address != null) {
      var addressType = address.type();
      var resAddrType = resource.addressType();
      Objects.requireNonNull(resAddrType); // just to satisfy errorprone
      ensure(addressType instanceof DataType,
          "Address must be a DataValue, was %s", address.type());
      ensure(((DataType) addressType).isTrivialCastTo(resAddrType),
          "Address value cannot be cast to resource's address type. %s vs %s",
          resource.addressType(), addressType);
    }

  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    if (this.address != null) {
      collection.add(address);
    }
    collection.add(value);
  }

  @Override
  public void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    address = visitor.applyNullable(this, address, ExpressionNode.class);
    value = visitor.apply(this, value, ExpressionNode.class);
  }

  /**
   * Returns the resource affected by this write node.
   */
  public abstract Resource resourceDefinition();

}
