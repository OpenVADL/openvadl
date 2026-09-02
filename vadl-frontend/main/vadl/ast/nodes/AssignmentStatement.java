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
import java.util.function.Consumer;
import vadl.utils.SourceLocation;

/**
 * An assignment statement that stores a value in a register, register file, or memory location.
 * The target (LHS) and value expression (RHS) are separated by the {@code :=} operator.
 */
@SuppressWarnings("MissingJavadocMethod")
public final class AssignmentStatement extends Statement {
  public Expr target;
  public Expr valueExpression;

  public AssignmentStatement(Expr target, Expr valueExpression) {
    this.target = target;
    this.valueExpression = valueExpression;
  }

  @Override
  public SourceLocation location() {
    return target.location().join(valueExpression.location());
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    target.prettyPrint(0, builder);
    if (isBlockLayout(valueExpression)) {
      builder.append(" :=\n");
    } else {
      builder.append(" := ");
    }
    valueExpression.prettyPrint(indent + 1, builder);
    builder.append("\n");
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    action.accept(target);
    action.accept(valueExpression);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (AssignmentStatement) obj;
    return Objects.equals(this.target, that.target)
        && Objects.equals(this.valueExpression, that.valueExpression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(target, valueExpression);
  }

  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
