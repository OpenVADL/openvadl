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
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings("MissingJavadocType")
public class EncodingDefinition extends Definition {
  @Child
  IdentifierOrPlaceholder instrIdentifier;
  @Child
  EncsNode encodings;
  SourceLocation loc;

  @Nullable
  FormatDefinition formatNode;

  EncodingDefinition(IdentifierOrPlaceholder instrIdentifier, EncsNode encodings,
                     SourceLocation location) {
    this.instrIdentifier = instrIdentifier;
    this.encodings = encodings;
    this.loc = location;
  }

  public Identifier identifier() {
    return (Identifier) instrIdentifier;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("encoding ");
    instrIdentifier.prettyPrint(0, builder);
    builder.append(" =\n");
    builder.append(prettyIndentString(indent)).append("{ ");
    encodings.prettyPrint(indent, builder);
    builder.append(prettyIndentString(indent)).append("}\n");
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

    var that = (EncodingDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(instrIdentifier, that.instrIdentifier)
        && Objects.equals(encodings, that.encodings);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(instrIdentifier);
    result = 31 * result + Objects.hashCode(encodings);
    return result;
  }

  static final class EncsNode extends Node implements IsEncs {
    @Child
    List<IsEncs> items;
    SourceLocation loc;

    EncsNode(List<IsEncs> items, SourceLocation loc) {
      this.items = items;
      this.loc = loc;
    }


    @Override
    public SourceLocation location() {
      return loc;
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.ENCS;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      boolean first = true;
      for (var entry : items) {
        if (!first) {
          builder.append(prettyIndentString(indent)).append(", ");
        }
        entry.prettyPrint(0, builder);
        builder.append("\n");
        first = false;
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      EncsNode that = (EncsNode) o;
      return Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(items);
    }
  }

  static final class EncodingField extends Node implements IsEncs {
    @Child
    final IdentifierOrPlaceholder field;
    @Child
    Expr value;

    EncodingField(IdentifierOrPlaceholder field, Expr value) {
      this.field = field;
      this.value = value;
    }

    Identifier identifier() {
      return (Identifier) field;
    }

    @Override
    public SourceLocation location() {
      return field.location().join(value.location());
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      field.prettyPrint(0, builder);
      builder.append(" = ");
      value.prettyPrint(0, builder);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (EncodingField) obj;
      return Objects.equals(this.field, that.field)
          && Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(field, value);
    }

    @Override
    public String toString() {
      return "EncodingField["
          + "field=" + field + ", "
          + "value=" + value + ']';
    }

  }
}
