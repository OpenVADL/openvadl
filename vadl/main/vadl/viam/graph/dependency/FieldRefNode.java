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
import vadl.types.DataType;
import vadl.viam.Definition;
import vadl.viam.Format;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * A node reference an instruction's format field.
 *
 * @see Format.Field
 */
public class FieldRefNode extends ParamNode {

  @DataValue
  protected Format.Field formatField;


  /**
   * Constructs a new {@link FieldRefNode} object with the given format field and data type.
   * The type of the formatField must be implicitly cast-able to the given type of the
   * node.
   *
   * @param formatField the format field of the instruction parameter
   * @param type        the data type of the instruction parameter
   */
  public FieldRefNode(Format.Field formatField, DataType type) {
    super(type);

    this.formatField = formatField;
  }

  public Format.Field formatField() {
    return formatField;
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(formatField.type().isTrivialCastTo((DataType) type()),
        "Format field type cannot be cast to node type: %s vs %s",
        formatField.type(), type());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(formatField);
  }

  @Override
  public ExpressionNode copy() {
    return new FieldRefNode(formatField, (DataType) type());
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public Definition definition() {
    return formatField;
  }

  @Override
  public void prettyPrint(StringBuilder sb) {
    sb.append(formatField.simpleName());
  }
}
