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
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

/**
 * A representation of terms of form {@code MEM<9>} or {@code VADL::fcvts::<IEEE32, 64>}.
 * These terms always have at least one argument in the pointy brackets.
 */
@SuppressWarnings("MissingJavadocMethod")
public final class SymbolExpr extends Expr implements IsSymExpr {
  @Child
  public IsId path;
  /**
   * The list of arguments in the pointy brackets. Always contains at least one element.
   */
  @Child
  public List<Expr> symbolArgs;
  public SourceLocation location;

  public SymbolExpr(IsId path, List<Expr> symbolArgs, SourceLocation location) {
    this.path = path;
    this.symbolArgs = symbolArgs;
    this.location = location;
  }

  @Override
  public IsId path() {
    return path;
  }

  @Override
  public List<Expr> symbolArgs() {
    return symbolArgs;
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.SYM_EX;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    path.prettyPrint(indent, builder);
    if (symbolArgs.size() > 1) {
      builder.append("::");
    }
    builder.append("< ");
    boolean first = true;
    for (var arg : symbolArgs) {
      if (!first) {
        builder.append(", ");
      }
      if (arg instanceof BinaryExpr) {
        builder.append("(");
      }
      arg.prettyPrintExpr(0, builder, Precedence.NoPrecedence);
      if (arg instanceof BinaryExpr) {
        builder.append(")");
      }
      first = false;
    }
    builder.append(" >");
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

    SymbolExpr that = (SymbolExpr) o;
    return path.equals(that.path) && symbolArgs.equals(that.symbolArgs);
  }

  @Override
  public int hashCode() {
    int result = path.hashCode();
    result = 31 * result + Objects.hashCode(symbolArgs);
    return result;
  }
}
