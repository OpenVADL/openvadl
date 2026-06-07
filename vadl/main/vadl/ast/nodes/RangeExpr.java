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

import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings("MissingJavadocType")
public class RangeExpr extends Expr {
  @Child
  Expr from;
  @Child
  Expr to;

  public RangeExpr(Expr from, Expr to) {
    this.from = from;
    this.to = to;
  }

  @Override
  public SourceLocation location() {
    return from.location().join(to.location());
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    from.prettyPrintExpr(indent, builder, Precedence.NoPrecedence);
    builder.append("..");
    to.prettyPrint(indent, builder);
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

    RangeExpr rangeExpr = (RangeExpr) o;
    return from.equals(rangeExpr.from) && to.equals(rangeExpr.to);
  }

  @Override
  public int hashCode() {
    int result = from.hashCode();
    result = 31 * result + to.hashCode();
    return result;
  }
}
