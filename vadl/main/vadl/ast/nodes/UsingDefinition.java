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
public class UsingDefinition extends Definition implements IdentifiableNode {
  final IdentifierOrPlaceholder id;
  @Child
  final TypeLiteral typeLiteral;
  final SourceLocation loc;

  UsingDefinition(IdentifierOrPlaceholder id, TypeLiteral typeLiteral, SourceLocation location) {
    this.id = id;
    this.typeLiteral = typeLiteral;
    this.loc = location;
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
    return BasicSyntaxType.COMMON_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("using ");
    id.prettyPrint(indent, builder);
    builder.append(" = ");
    typeLiteral.prettyPrint(indent, builder);
    builder.append("\n");
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

    var that = (UsingDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(id, that.id)
        && Objects.equals(typeLiteral, that.typeLiteral);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(id);
    result = 31 * result + Objects.hashCode(typeLiteral);
    return result;
  }
}
