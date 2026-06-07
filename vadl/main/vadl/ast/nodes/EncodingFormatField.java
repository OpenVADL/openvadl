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

import java.util.function.Consumer;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings("MissingJavadocType")
public class EncodingFormatField extends FormatField {
  @Child
  Expr expr;

  public EncodingFormatField(IdentifierOrPlaceholder identifier, Expr expr) {
    super(identifier);
    this.expr = expr;
  }

  /**
   * Returns the field for which this is a predicate.
   *
   * @return the field.
   */
  FormatField target() {
    return (FormatField) requireNonNull(identifier.target());
  }

  @Override
  void forEachChild(Consumer<Node> action) {
    // This has to be hardcoded here because for this format field it's a child but for some it's
    // the identfiyable name.
    action.accept((Node) identifier);
    action.accept(expr);
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    identifier.prettyPrint(indent, builder);
    builder.append(" := ");
    expr.prettyPrint(indent, builder);
  }

  @Override
  public SourceLocation location() {
    return identifier.location().join(expr.location());
  }

  @Override
  public final boolean equals(Object o) {
    if (!(o instanceof EncodingFormatField that)) {
      return false;
    }

    return identifier.equals(that.identifier) && expr.equals(that.expr);
  }

  @Override
  public int hashCode() {
    int result = identifier.hashCode();
    result = 31 * result + expr.hashCode();
    return result;
  }
}
