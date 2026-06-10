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

package vadl.ast.nodes;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

/**
 * TypeLiterals are needed as the types are not known during parsing.
 * For example {@code Bits<counter>} depends on the constant {@code counter} used here and so some
 * constant evaluation has to be performed for the concrete type to be known here.
 */
@SuppressWarnings("MissingJavadocMethod")
public final class TypeLiteral extends Expr {
  @Child
  public IsId baseType;

  /**
   * The sizes of the type literal.
   * Can be zero or more, written like {@code Bits<dimension1><dimension2><dimension3>} etc.
   */
  @Child
  public List<Expr> sizeIndices;

  public SourceLocation loc;

  public TypeLiteral(IsId baseType, List<Expr> sizeIndices, SourceLocation loc) {
    this.baseType = baseType;
    this.sizeIndices = sizeIndices;
    this.loc = loc;
  }

  public TypeLiteral(IsSymExpr symExpr) {
    this.baseType = symExpr.path();
    var size = symExpr.size();
    this.sizeIndices = size == null ? List.of() : List.of(size);
    this.loc = symExpr.location();
  }

  /**
   * For builtin types this won't return anything, but for custom types it returns the definition
   * the type literal points to. For example users can introduce new custom types with
   * using or format definitions.
   *
   * @return the target of the type literal
   */
  @Nullable
  public Node target() {
    return this.baseType.target();
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append(baseType.pathToString());
    for (var index : sizeIndices) {
      builder.append("<");
      index.prettyPrint(indent, builder);
      builder.append(">");
    }
  }

  @Override
  public <R> R accept(ExprVisitor<R> visitor) {
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

    TypeLiteral that = (TypeLiteral) o;
    return baseType.equals(that.baseType)
        && Objects.equals(sizeIndices, that.sizeIndices);
  }

  @Override
  public int hashCode() {
    int result = baseType.hashCode();
    result = 31 * result + Objects.hashCode(sizeIndices);
    return result;
  }
}
