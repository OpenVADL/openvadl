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

package vadl.rtl.ipg.nodes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import vadl.javaannotations.viam.DataValue;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Node representing a bit slice of the instruction word. It is used to convert
 * {@link vadl.viam.graph.dependency.FieldRefNode}s to simple bit slices of the instruction word,
 * which are unique across formats.
 */
public class InstructionWordSliceNode extends ExpressionNode {

  @DataValue
  protected BitsType formatType;

  @DataValue
  protected Constant.BitSlice slice;

  private final Set<Format.Field> fields;

  /**
   * Create a new instruction word slice node.
   *
   * @param formatType format type (type of the instruction word)
   * @param slice bit slice
   * @param type data type of the slice result
   */
  public InstructionWordSliceNode(BitsType formatType, Constant.BitSlice slice, DataType type) {
    super(type);
    this.formatType = formatType;
    this.slice = slice;
    this.fields = new HashSet<>();
  }

  /**
   * Create a new instruction word slice node.
   *
   * @param formatType format type (type of the instruction word)
   * @param slice bit slice
   * @param fields set of format fields
   * @param type data type of the slice result
   */
  public InstructionWordSliceNode(BitsType formatType, Constant.BitSlice slice,
                                  Set<Format.Field> fields, DataType type) {
    super(type);
    this.formatType = formatType;
    this.slice = slice;
    this.fields = fields;
  }

  /**
   * Get the instruction format type.
   *
   * @return format type
   */
  public BitsType formatType() {
    return formatType;
  }

  /**
   * Get bit slice.
   *
   * @return bit slice
   */
  public Constant.BitSlice slice() {
    return slice;
  }

  /**
   * Get set of fields associated with this instruction word slice.
   *
   * @return set of field definitions
   */
  public Set<Format.Field> fields() {
    return fields;
  }

  /**
   * Add a format field to the instruction word slice node.
   * This field must match the format type and bit slice of this node.
   *
   * @param field field definition
   */
  public void addField(Format.Field field) {
    ensure(field.format().type().equals(formatType),
        "Fields in instruction word slice must have matching format types");
    ensure(field.bitSlice().equals(slice),
        "Fields in instruction word slice must have matching bit slices");
    fields.add(field);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(formatType);
    collection.add(slice);
  }

  @Override
  public ExpressionNode copy() {
    return new InstructionWordSliceNode(formatType, slice, fields, type().asDataType());
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }

}
