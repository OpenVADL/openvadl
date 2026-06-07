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
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings("MissingJavadocType")
public class CacheDefinition extends Definition implements IdentifiableNode {
  IdentifierOrPlaceholder id;
  @Child
  TypeLiteral sourceType;
  @Child
  TypeLiteral targetType;
  SourceLocation loc;

  CacheDefinition(IdentifierOrPlaceholder id, TypeLiteral sourceType, TypeLiteral targetType,
                  SourceLocation loc) {
    this.id = id;
    this.sourceType = sourceType;
    this.targetType = targetType;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("cache ");
    id.prettyPrint(0, builder);
    builder.append(" : ");
    sourceType.prettyPrint(0, builder);
    builder.append(" -> ");
    targetType.prettyPrint(0, builder);
    builder.append("\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CacheDefinition that = (CacheDefinition) o;
    return Objects.equals(id, that.id)
        && Objects.equals(sourceType, that.sourceType)
        && Objects.equals(targetType, that.targetType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, sourceType, targetType);
  }

  @Override
  public Identifier identifier() {
    return (Identifier) id;
  }
}
