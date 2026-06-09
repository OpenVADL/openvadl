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
 * A statement to do repeated work in a loop, mostly used for tensors.
 *
 * <p>{@code
 * // initialize 4 consecutive X registers
 * instruction Init4X : F = forall i: Bits<8> in 0 .. 3 do
 * X(rd + i) := 0
 * }}
 */
@SuppressWarnings("MissingJavadocMethod")
public final class ForallStatement extends Statement {
  @Child
  public List<ForallIndex> indices;

  @Child
  public Statement body;

  public SourceLocation loc;

  public ForallStatement(List<ForallIndex> indices, Statement body, SourceLocation loc) {
    this.indices = indices;
    this.body = body;
    this.loc = loc;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("forall ");
    var isFirst = true;
    for (var index : indices) {
      if (!isFirst) {
        builder.append(", ");
      }
      index.prettyPrint(indent, builder);
    }
    builder.append(" do\n");
    body.prettyPrint(indent + 1, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ForallStatement that = (ForallStatement) o;
    return Objects.equals(indices, that.indices)
        && Objects.equals(body, that.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(indices, body);
  }

  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visit(this);
  }

}
