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
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class GroupedExpr extends Expr {
  @Child
  public List<Expr> expressions;
  public SourceLocation loc;

  public GroupedExpr(List<Expr> expressions, SourceLocation loc) {
    this.expressions = expressions;
    this.loc = loc;
  }

  @Override
  public <R> R accept(ExprVisitor<R> visitor) {
    // This node should never leave the parser and therefore never meet a visitor.
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append("(");
    var isFirst = true;
    for (var expr : expressions) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      expr.prettyPrintExpr(0, builder, Precedence.NoPrecedence);
    }
    builder.append(")");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    GroupedExpr that = (GroupedExpr) o;
    return expressions.equals(that.expressions);
  }

  @Override
  public int hashCode() {
    return expressions.hashCode();
  }
}
