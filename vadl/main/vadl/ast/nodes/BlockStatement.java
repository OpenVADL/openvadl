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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings("MissingJavadocType")
public final class BlockStatement extends Statement {
  @Child
  List<Statement> statements;
  SourceLocation location;

  BlockStatement(List<Statement> statements, SourceLocation location) {
    this.statements = statements;
    this.location = location;
  }

  BlockStatement(SourceLocation location) {
    this(new ArrayList<>(), location);
  }

  BlockStatement add(Statement statement) {
    statements.add(statement);
    return this;
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("{\n");
    statements.forEach(statement -> statement.prettyPrint(indent + 1, builder));
    builder.append(prettyIndentString(indent));
    builder.append("}\n");
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (BlockStatement) obj;
    return Objects.equals(this.statements, that.statements);
  }

  @Override
  public int hashCode() {
    return Objects.hash(statements);
  }

  @Override
  <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
