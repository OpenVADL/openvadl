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
public final class LockStatement extends Statement {
  @Child
  Expr expr;
  @Child
  Statement statement;
  SourceLocation loc;

  LockStatement(Expr expr, Statement statement, SourceLocation loc) {
    this.expr = expr;
    this.statement = statement;
    this.loc = loc;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("lock ");
    expr.prettyPrint(0, builder);
    builder.append(" in\n");
    statement.prettyPrint(indent + 1, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LockStatement that = (LockStatement) o;
    return Objects.equals(expr, that.expr)
        && Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    return Objects.hash(expr, statement);
  }

  @Override
  <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
