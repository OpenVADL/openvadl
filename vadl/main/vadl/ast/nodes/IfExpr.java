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
public class IfExpr extends Expr {
  @Child
  Expr condition;
  @Child
  Expr thenExpr;
  @Child
  Expr elseExpr;
  SourceLocation location;

  IfExpr(Expr condition, Expr thenExpr, Expr elseExpr, SourceLocation location) {
    this.condition = condition;
    this.thenExpr = thenExpr;
    this.elseExpr = elseExpr;
    this.location = location;
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append(prettyIndentString(indent));
    builder.append("if ");
    condition.prettyPrintExpr(indent, builder, Precedence.NoPrecedence);
    builder.append(" then\n");
    if (!isBlockLayout(thenExpr)) {
      builder.append(prettyIndentString(indent + 1));
    }
    thenExpr.prettyPrintExpr(indent + 1, builder, Precedence.NoPrecedence);
    builder.append("\n").append(prettyIndentString(indent)).append("else\n");
    if (!isBlockLayout(elseExpr)) {
      builder.append(prettyIndentString(indent + 1));
    }
    elseExpr.prettyPrintExpr(indent + 1, builder, Precedence.NoPrecedence);
  }

  @Override
  <R> R accept(ExprVisitor<R> visitor) {
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

    IfExpr that = (IfExpr) o;
    return condition.equals(that.condition)
        && thenExpr.equals(that.thenExpr)
        && elseExpr.equals(that.elseExpr);
  }

  @Override
  public int hashCode() {
    int result = condition.hashCode();
    result = 31 * result + Objects.hashCode(thenExpr);
    result = 31 * result + Objects.hashCode(elseExpr);
    return result;
  }
}
