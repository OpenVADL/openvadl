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

import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.List;
import java.util.Objects;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

/**
 * Marks the following statement as exceptional code.
 * After the raise statement finishes, no further instruction behavior is executed.
 *
 * @see ExceptionDefinition
 */
@SuppressWarnings("MissingJavadocMethod")
public final class RaiseStatement extends Statement {

  @Child
  public Statement statement;
  public SourceLocation location;

  @LazyInit
  public List<String> viamId;

  public RaiseStatement(Statement statement, SourceLocation location) {
    this.statement = statement;
    this.location = location;
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append("raise ");
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
    RaiseStatement that = (RaiseStatement) o;
    return Objects.equals(statement, that.statement);
  }

  @Override
  public int hashCode() {
    return Objects.hash(statement, location);
  }

  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
