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
import vadl.viam.Constant;
import vadl.viam.graph.Canonicalizable;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * A slice of a single range with dynamic range indices.
 * This is required as a {@link SliceNode} may only represent constant slice indices.
 * However, in case of a {@link vadl.viam.graph.control.ForallNode} and similar, the index
 * {@link ForIdxNode} might be used as part of the slice indices, and thus we need
 * this dynamic slice if the ForIdx is part of the slice index expression.
 * However, there must be no other resource (except for constants) in the index expression!
 * Also, this dynamic slice only represents a single range (with one msb and one lsb), compared
 * to the {@link SliceNode} which may represent multiple ranges as part of its
 * {@link vadl.viam.Constant.BitSlice}.
 *
 * <p><b>Note:</b> It is possible for a {@code SliceNode} to express a slice that is out of range
 * relative to the value being sliced since the typechecker isn't able to verify the correctness
 * here.
 */
public class DynSliceNode extends ExpressionNode implements Canonicalizable {

  @Input
  protected ExpressionNode value;

  @Input
  protected ExpressionNode msb;

  @Input
  protected ExpressionNode lsb;


  /**
   * Constructs a new SliceNode.
   *
   * @param value The value from which the bit slice is taken.
   * @param msb   The msb bit index expression to slice the value from.
   * @param lsb   The lsb bit index expression to slice the value from.
   * @param type  The result type of the node.
   */
  public DynSliceNode(ExpressionNode value, ExpressionNode msb, ExpressionNode lsb, DataType type) {
    super(type);

    this.value = value;
    this.msb = msb;
    this.lsb = lsb;
  }

  public ExpressionNode value() {
    return value;
  }

  public ExpressionNode msb() {
    return msb;
  }

  public ExpressionNode lsb() {
    return lsb;
  }

  @Override
  public DataType type() {
    return (DataType) super.type();
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(value);
    collection.add(msb);
    collection.add(lsb);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    value = visitor.apply(this, value, ExpressionNode.class);
    msb = visitor.apply(this, msb, ExpressionNode.class);
    lsb = visitor.apply(this, lsb, ExpressionNode.class);
  }

  @Override
  public ExpressionNode copy() {
    return new DynSliceNode(value.copy(), msb.copy(), lsb.copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new DynSliceNode(value, msb, lsb, type());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public Node canonical() {
    if (msb.isConstant() && lsb.isConstant()) {
      // if the slice range is constant we can convert it to a SliceNode
      var msbVal = ((ConstantNode) msb).constant().asVal().unsignedInteger().intValue();
      var lsbVal = ((ConstantNode) lsb).constant().asVal().unsignedInteger().intValue();
      // set msb/lsb according to their value size
      if (msbVal <= lsbVal) {
        var tmp = msbVal;
        msbVal = lsbVal;
        lsbVal = tmp;
      }

      var sliceNode = new SliceNode(value, Constant.BitSlice.of(msbVal, lsbVal), type());
      // if the value itself is constant, it can be constant folded.
      return sliceNode.canonical();
    }

    return this;
  }
}
