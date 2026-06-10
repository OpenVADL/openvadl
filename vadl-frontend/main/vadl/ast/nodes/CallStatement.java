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

import java.util.Objects;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

/**
 * A standalone call statement (e.g. a function or instruction call without assignment).
 * Used for pseudo instruction definitions and ABI sequence definitions.
 */
@SuppressWarnings("MissingJavadocMethod")
public final class CallStatement extends Statement {

  @Child
  public Expr expr;

  public CallStatement(Expr expr) {
    this.expr = expr;
  }

  @Override
  public SourceLocation location() {
    return expr.location();
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
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
    CallStatement that = (CallStatement) o;
    return Objects.equals(expr, that.expr);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(expr);
  }

  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
