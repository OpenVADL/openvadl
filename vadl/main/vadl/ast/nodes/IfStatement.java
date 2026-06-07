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
import javax.annotation.Nullable;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings("MissingJavadocType")
public final class IfStatement extends Statement {
  @Child
  Expr condition;
  @Child
  Statement thenStmt;
  @Nullable
  @Child
  Statement elseStmt;
  SourceLocation location;

  IfStatement(Expr condition, Statement thenStmt, @Nullable Statement elseStmt,
              SourceLocation location) {
    this.condition = condition;
    this.thenStmt = thenStmt;
    this.elseStmt = elseStmt;
    this.location = location;
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("if ");
    condition.prettyPrint(indent + 1, builder);
    builder.append(" then\n");
    thenStmt.prettyPrint(indent + 1, builder);
    builder.append("\n");
    if (elseStmt != null) {
      builder.append(prettyIndentString(indent));
      builder.append("else\n");
      elseStmt.prettyPrint(indent + 1, builder);
      builder.append("\n");
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (IfStatement) obj;
    return Objects.equals(this.condition, that.condition)
        && Objects.equals(this.thenStmt, that.thenStmt)
        && Objects.equals(this.elseStmt, that.elseStmt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(condition, thenStmt, elseStmt);
  }

  @Override
  <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
