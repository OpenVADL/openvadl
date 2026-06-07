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

package vadl.ast;

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.javaannotations.ast.Child;
import vadl.types.Type;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class FormatDefinition extends Definition implements IdentifiableNode, TypedNode {
  IdentifierOrPlaceholder identifier;
  @Child
  TypeLiteral typeLiteral;
  @Child
  List<FormatField> fields;
  SourceLocation loc;

  @Override
  public Type type() {
    return new FormatType(this);
  }

  record BitRange(int from, int to) {
  }


  public FormatDefinition(IdentifierOrPlaceholder identifier, TypeLiteral typeLiteral,
                          List<FormatField> fields, SourceLocation location) {
    this.identifier = identifier;
    this.typeLiteral = typeLiteral;
    this.fields = fields;
    this.loc = location;
  }

  Stream<FormatField> fieldsWithoutEncodingPredicate() {
    return fields.stream()
        .filter(f -> !(f instanceof PredicateFormatField))
        .filter(f -> !(f instanceof EncodingFormatField));
  }

  boolean hasField(String name) {
    for (var field : fields) {
      if (field instanceof PredicateFormatField || field instanceof EncodingFormatField) {
        continue;
      }
      if (field.identifier().name.equals(name)) {
        return true;
      }
    }
    return false;
  }

  FormatField getField(String name) {
    for (var field : fields) {
      if (field instanceof PredicateFormatField || field instanceof EncodingFormatField) {
        continue;
      }
      if (field.identifier().name.equals(name)) {
        return field;
      }
    }
    throw new IllegalArgumentException("Field with name '" + name + "' not found");
  }

  @Nullable
  Type getFieldType(String name) {
    var field = getField(name);

    if (field instanceof TypedFormatField typedField) {
      return typedField.typeLiteral.type;
    } else if (field instanceof RangeFormatField rangeField) {
      return rangeField.type;
    } else if (field instanceof DerivedFormatField derivedField) {
      return derivedField.expr.type;
    } else {
      throw new IllegalStateException("Unknown field type: " + field.getClass().getSimpleName());
    }
  }

  @Nullable
  BitRange getFieldRange(String name) {
    var field = getField(name);

    if (field instanceof TypedFormatField typedField) {
      return typedField.range;
    } else if (field instanceof RangeFormatField rangeField) {
      // FIXME: propper merge them
      if (rangeField.ranges.size() > 1) {
        throw new IllegalStateException(
            "Not implemented: Too many ranges: " + rangeField.ranges.size());
      }
      if (rangeField.computedRanges == null) {
        return null;
      }
      return rangeField.computedRanges.get(0);
    } else if (field instanceof DerivedFormatField) {
      throw new IllegalStateException(
          "Cannot compute range of derived field: " + field.getClass().getSimpleName());
    } else {
      throw new IllegalStateException("Unknown field type: " + field.getClass().getSimpleName());
    }
  }

  @Override
  public Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.COMMON_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("format ");
    identifier.prettyPrint(indent, builder);
    builder.append(": ");
    typeLiteral.prettyPrint(indent, builder);

    if (fields.isEmpty()) {
      builder.append("\n");
      return;
    }

    builder.append(" =\n");

    builder.append(prettyIndentString(indent));
    builder.append("{ ");

    fields.get(0).prettyPrint(indent, builder);
    builder.append("\n");

    for (int i = 1; i < fields.size(); i++) {
      builder.append(prettyIndentString(indent));
      builder.append(", ");
      fields.get(i).prettyPrint(indent, builder);
      builder.append("\n");

    }

    builder.append(prettyIndentString(indent));
    builder.append("}\n");
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    FormatDefinition that = (FormatDefinition) o;
    return annotations.equals(that.annotations)
        && identifier.equals(that.identifier)
        && typeLiteral.equals(that.typeLiteral)
        && fields.equals(that.fields);
  }

  @Override
  public int hashCode() {
    int result = annotations.hashCode();
    result = 31 * result + identifier.hashCode();
    result = 31 * result + typeLiteral.hashCode();
    result = 31 * result + fields.hashCode();
    return result;
  }
}
