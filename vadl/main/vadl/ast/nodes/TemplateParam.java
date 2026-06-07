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

import java.util.Objects;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class TemplateParam extends Node implements IdentifiableNode {
  IdentifierOrPlaceholder name;
  TypeLiteral type;

  @Nullable
  Expr value;

  public TemplateParam(IdentifierOrPlaceholder name, TypeLiteral type, @Nullable Expr value) {
    this.name = name;
    this.type = type;
    this.value = value;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) name;
  }

  @Override
  public SourceLocation location() {
    if (value != null) {
      return name.location().join(value.location());
    }
    return name.location().join(type.location());
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    name.prettyPrint(0, builder);
    builder.append(": ");
    type.prettyPrint(0, builder);
    if (value != null) {
      builder.append(" = ");
      value.prettyPrint(0, builder);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TemplateParam that = (TemplateParam) o;
    return name.equals(that.name) && type.equals(that.type)
        && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + Objects.hashCode(value);
    return result;
  }
}
