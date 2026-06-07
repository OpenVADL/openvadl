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

import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.javaannotations.ast.Child;
import vadl.types.Type;
import vadl.utils.SourceLocation;

@SuppressWarnings("MissingJavadocType")
public final class EnumerationDefinition extends Definition implements IdentifiableNode {
  IdentifierOrPlaceholder id;
  @Nullable
  @Child
  TypeLiteral enumType;
  @Child
  List<Entry> entries;
  SourceLocation loc;

  EnumerationDefinition(IdentifierOrPlaceholder id, @Nullable TypeLiteral enumType,
                        List<Entry> entries, SourceLocation location) {
    this.id = id;
    this.enumType = enumType;
    entries.forEach(e -> e.enumDef = this);
    this.entries = entries;
    this.loc = location;
  }

  Entry getEntry(String name) {
    return entries.stream().filter(e -> e.identifier().name.equals(name)).findFirst().orElseThrow();
  }

  Expr getEntryValue(String name) {
    return requireNonNull(getEntry(name).value);
  }

  Type getEntryType(String name) {
    return requireNonNull(getEntry(name).value).type();
  }

  @Override
  public Identifier identifier() {
    return (Identifier) id;
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
    builder.append(prettyIndentString(indent)).append("enumeration ");
    id.prettyPrint(0, builder);
    if (enumType != null) {
      builder.append(" : ");
      enumType.prettyPrint(0, builder);
    }
    builder.append(" =\n");
    builder.append(prettyIndentString(indent + 1)).append("{ ");
    var isFirst = true;
    for (var entry : entries) {
      if (!isFirst) {
        builder.append(prettyIndentString(indent + 1)).append(", ");
      }
      isFirst = false;
      entry.name.prettyPrint(0, builder);
      if (entry.value != null) {
        builder.append(" = ");
        entry.value.prettyPrint(0, builder);
      }
      builder.append("\n");
    }
    builder.append(prettyIndentString(indent + 1)).append("}\n");
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

    var that = (EnumerationDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(id, that.id)
        && Objects.equals(enumType, that.enumType)
        && Objects.equals(entries, that.entries);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(id);
    result = 31 * result + Objects.hashCode(enumType);
    result = 31 * result + Objects.hashCode(entries);
    return result;
  }

  // FIXME: this should be a definition.
  static class Entry extends Node implements IdentifiableNode {

    IdentifierOrPlaceholder name;

    /**
     * Potentially set by the typechecker if no value was explicitly assigned.
     * In that case the typechecker will increment the value of the last entry by one and insert
     * it here as a {@link IntegerLiteral}.
     */
    @Nullable
    @Child
    Expr value;

    /**
     * Points to the parent definition of the entry, is set in the constructor of the parent.
     */
    @LazyInit
    EnumerationDefinition enumDef;


    public Entry(IdentifierOrPlaceholder name, @Nullable Expr value) {
      this.name = name;
      this.value = value;
    }

    @Override
    public SourceLocation location() {
      var loc = name.location();
      if (value != null) {
        loc = loc.join(value.location());
      }
      return loc;
    }

    @Override
    SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    void prettyPrint(int indent, StringBuilder builder) {
      name.prettyPrint(indent, builder);
      builder.append(" = ");
      if (value != null) {
        value.prettyPrint(indent, builder);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Entry entry = (Entry) o;
      return name.equals(entry.name) && Objects.equals(value, entry.value);
    }

    @Override
    public int hashCode() {
      int result = name.hashCode();
      result = 31 * result + Objects.hashCode(value);
      return result;
    }

    @Override
    public Identifier identifier() {
      return (Identifier) name;
    }
  }
}
