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

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import javax.annotation.Nullable;
import vadl.javaannotations.ast.Child;
import vadl.types.Type;
import vadl.utils.SourceLocation;

/**
 * A type cast expression using the {@code as} keyword.
 * Performs explicit type conversions between VADL types, e.g. for signed/unsigned arithmetic.
 *
 * <p>Example: {@code value as UInt<32>}
 */
@SuppressWarnings("MissingJavadocMethod")
public class CastExpr extends Expr {
  @Child
  public Expr value;

  /**
   * The typeLiteral.
   * The typechecker also expands implicit casts to explicit ones and often inserts new
   * CastExpressions. Since these don't come from the source code, this field is left empty in
   * such cases, and only the Type field is used.
   */
  @Nullable
  @Child
  public TypeLiteral typeLiteral;

  public SourceLocation location;

  public CastExpr(Expr value, TypeLiteral typeLiteral) {
    this.value = value;
    this.typeLiteral = typeLiteral;
    this.location = value.location().join(typeLiteral.location());
  }

  /**
   * A syntetic constructor that doesn't originate from the source code.
   *
   * @param value to be cast.
   * @param type to which it is cast.
   */
  public CastExpr(Expr value, Type type) {
    this.value = value;
    this.type = type;
    this.location = value.location();
  }

  @Override
  public Precedence precedence() {
    return Precedence.CastOp;
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    wrapInGroup(parentPrec, builder, true, () -> {
      value.prettyPrintExpr(indent, builder, Precedence.CastOp);
      builder.append(" as ");
      if (typeLiteral != null) {
        typeLiteral.prettyPrint(indent, builder);
      } else {
        builder.append(requireNonNull(type));
      }
    });
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

    CastExpr that = (CastExpr) o;
    return value.equals(that.value) && Objects.equals(typeLiteral, that.typeLiteral);
  }

  @Override
  public int hashCode() {
    int result = value.hashCode();
    result = 31 * result + Objects.hashCode(typeLiteral);
    return result;
  }
}
