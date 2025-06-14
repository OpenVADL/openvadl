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

package vadl.viam;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.types.Type;
import vadl.viam.graph.Graph;

/**
 * The encoding for a specific instruction.
 *
 * <p>It holds instruction encoded fields available to the instruction.
 * Each field has a reference to the original format field definition and
 * a constant that defines the encoding.</p>
 */
public class Encoding extends Definition implements DefProp.WithType {

  private final Format format;

  private final Field[] fieldEncodings;
  private final Format.Field[] nonEncodedFormatFields;

  // The constraint graph is a function graph that accesses format fields.
  // It is set with the `select when: <expr>` annotation and specifies under which conditions
  // an instruction encoding is valid.
  // If it is null, there are no conditions.
  @Nullable
  private Graph constraint;

  /**
   * Constructs the encoding of an {@link Instruction}.
   *
   * @param identifier     The identifier of the encoding.
   * @param format         The format of the encoding.
   * @param fieldEncodings The field encodings.
   */
  public Encoding(Identifier identifier, Format format, Field[] fieldEncodings) {
    super(identifier);
    this.format = format;
    this.fieldEncodings = fieldEncodings;
    this.nonEncodedFormatFields = determineNoneEncodedFields(format, fieldEncodings);
  }

  @Override
  public Type type() {
    return format.type();
  }

  public Field[] fieldEncodings() {
    return fieldEncodings;
  }

  /**
   * Returns all non-encoded format fields.
   *
   * <p>This method filters the fields of the given format and returns only the fields
   * that are not present in the set of encoded fields.</p>
   */
  public Format.Field[] nonEncodedFormatFields() {
    return nonEncodedFormatFields;
  }

  /**
   * Returns the field encoding for the given format field.
   *
   * @param formatField The format field to search for.
   * @return The field encoding for the given format field, or null if not found.
   */
  public @Nullable Field fieldEncodingOf(Format.Field formatField) {
    for (Field fieldEncoding : fieldEncodings) {
      if (fieldEncoding.formatField.equals(formatField)) {
        return fieldEncoding;
      }
    }
    return null;
  }

  public Format format() {
    return format;
  }

  @Nullable
  public Graph constraint() {
    return constraint;
  }

  public void setConstraint(@Nullable Graph constraint) {
    this.constraint = constraint;
  }

  @Override
  public String toString() {
    return "Encoding{ " + identifier + " = {\n\t"
        + Stream.of(fieldEncodings).map(Field::toString).collect(
        Collectors.joining(",\n\t")) + " \n}}";
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * Determines what format fields are not encoded by with the given fieldEncodings.
   * E.i. all fields in the format that are not referenced by any field of the field encodings.
   */
  private static Format.Field[] determineNoneEncodedFields(Format format, Field[] fieldEncodings) {
    var nonEncodedFormatFields = new ArrayList<Format.Field>();
    // determine all format fields that are not encoded by this encoding
    for (Format.Field formatField : format.fields()) {
      boolean found = false;
      for (Field encField : fieldEncodings) {
        if (encField.formatField.equals(formatField)) {
          found = true;
          break;
        }
      }
      if (!found) {
        nonEncodedFormatFields.add(formatField);
      }
    }
    return nonEncodedFormatFields.toArray(Format.Field[]::new);
  }


  /**
   * A field of a VADL encoding.
   * Holds information about the format field, constant value, and type of the field.
   */
  public static class Field extends Definition {

    private final Format.Field formatField;
    private final Constant.Value constant;

    /**
     * Constructs a new Field object with the given identifier, format field, and constant value.
     * The type of the constant must be implicitly cast able to the type of the format field.
     *
     * @param identifier  the identifier of the field
     * @param formatField the format field of the field
     * @param constant    the constant value of the field
     */
    public Field(Identifier identifier, Format.Field formatField, Constant.Value constant) {
      super(identifier);

      this.formatField = formatField;
      this.constant = constant;

      verify();
    }

    public Format.Field formatField() {
      return formatField;
    }

    public Type type() {
      return formatField.type();
    }

    public Constant.Value constant() {
      return constant;
    }

    @Override
    public void verify() {
      super.verify();
      ensure(constant.type().isTrivialCastTo(formatField.type()),
          "Constant is of type %s, but format field is of type %s which cannot be cast implicit",
          constant.type(),
          formatField.type());
    }

    @Override
    public String toString() {
      return "Field{ " + identifier + " = " + constant + " }";
    }

    @Override
    public int hashCode() {
      int result = formatField.hashCode();
      result = 31 * result + constant.hashCode();
      return result;
    }

    @Override
    public void accept(DefinitionVisitor visitor) {
      visitor.visit(this);
    }
  }

}
