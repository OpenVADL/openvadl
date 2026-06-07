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
public class DerivedFormatField extends FormatField implements IdentifiableNode {
  @Child
  Expr expr;

  public DerivedFormatField(IdentifierOrPlaceholder identifier, Expr expr) {
    super(identifier);
    this.expr = expr;
  }

  @Override
  public SourceLocation location() {
    return identifier.location().join(expr.location());
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    identifier.prettyPrint(indent, builder);
    builder.append(" = ");
    expr.prettyPrint(indent, builder);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DerivedFormatField that = (DerivedFormatField) o;
    return Objects.equals(identifier, that.identifier)
        && Objects.equals(expr, that.expr);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(expr);
    return result;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public Identifier identifier() {
    return (Identifier) identifier;
  }
}
