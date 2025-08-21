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

package vadl.gcb.passes;

import vadl.types.DataType;
import vadl.viam.Format;
import vadl.viam.Identifier;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;

/**
 * A conflict occurs if you read a register which is going to be written. In these cases,
 * we need to rename the field. The compiler backend can then declare a constraint so they
 * remain the same.
 */
public class RenamedFieldRefNode extends FieldRefNode {
  private final int counter;
  private final Format.Field original;

  /**
   * Constructs a new {@link FieldRefNode} object with the given format field and data type.
   * The type of the formatField must be implicitly cast-able to the given type of the
   * node.
   *
   * @param formatField the format field of the instruction parameter
   * @param type        the data type of the instruction parameter
   * @param counter     to avoid naming conflicts.
   */
  public RenamedFieldRefNode(Format.Field formatField, DataType type, int counter) {
    super(formatField, type);
    this.original = formatField;
    this.formatField = new RenamedField(formatField, counter);
    this.counter = counter;
  }

  private RenamedFieldRefNode(Format.Field formatField,
                              Format.Field original,
                              DataType type,
                              int counter) {
    super(original, type);
    this.original = original;
    this.formatField = formatField;
    this.counter = counter;
  }

  public Format.Field originalField() {
    return original;
  }

  public Format.Field replaced() {
    return formatField;
  }

  @Override
  public ExpressionNode copy() {
    return new RenamedFieldRefNode(formatField, original, type().asDataType(), counter);
  }

  @Override
  public Node shallowCopy() {
    return new RenamedFieldRefNode(formatField, original, type().asDataType(), counter);
  }

  /**
   * When a register is both read and written, the compiler has a conflict. In that case, we need
   * to rename the field and add a constraint.
   */
  public static class RenamedField extends Format.Field {
    private final Format.Field inner;

    /**
     * Constructs a Field object.
     */
    public RenamedField(Format.Field field, int counter) {
      super(new Identifier(field.simpleName() + "_" + counter, field.location()),
          field.type(),
          field.bitSlice(),
          field.format());
      this.inner = field;
    }

    public Format.Field inner() {
      return inner;
    }
  }
}
