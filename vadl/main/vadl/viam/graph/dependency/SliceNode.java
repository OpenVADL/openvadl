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
import vadl.viam.Constant;
import vadl.viam.graph.Canonicalizable;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * A node that represents the bit slice operation on a value.
 */
public class SliceNode extends ExpressionNode implements Canonicalizable {

  @DataValue
  protected Constant.BitSlice slice;

  @Input
  protected ExpressionNode value;

  /**
   * Constructs a new SliceNode.
   *
   * @param value The value from which the bit slice is taken.
   * @param slice The bit slice represented by this node.
   * @param type  The result type of the node.
   */
  public SliceNode(ExpressionNode value, Constant.BitSlice slice, DataType type) {
    super(type);

    this.value = value;
    this.slice = slice;
  }

  public Constant.BitSlice bitSlice() {
    return slice;
  }

  public ExpressionNode value() {
    return value;
  }

  @Override
  public DataType type() {
    return (DataType) super.type();
  }

  public void setSlice(Constant.BitSlice slice) {
    this.slice = slice;
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(Type.bits(slice.bitSize()).isTrivialCastTo(type()),
        "Slice type cannot be cast to node type: %s vs %s",
        Type.bits(slice.bitSize()), type());
    ensure(value.type() instanceof DataType, "Value node must have a data type.");
    ensure(((DataType) value.type()).bitWidth() > slice.msb(),
        "Value node must have at least %d bits to be sliceable by %s",
        slice.msb() + 1, slice);
  }

  @Override
  public void prettyPrint(StringBuilder sb) {
    value.prettyPrint(sb);
    sb.append(slice);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(slice);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(value);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    value = visitor.apply(this, value, ExpressionNode.class);
  }

  @Override
  public ExpressionNode copy() {
    return new SliceNode((ExpressionNode) value.copy(), slice, type());
  }

  @Override
  public Node shallowCopy() {
    return new SliceNode(value, slice, type());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public Node canonical() {
    if (!(value instanceof ConstantNode constantNode)) {
      return this;
    }
    var val = constantNode.constant.asVal();
    var result = val.slice(slice).toNode();
    ensure(result.type().isTrivialCastTo(type()), "Slice result has different width to node");
    return result;
  }
}
