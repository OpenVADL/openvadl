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
 * An existential quantifier expression over a set of operations.
 * Written as {@code exists in {op1, op2, ...}}.
 * Evaluates to true if the current context matches any of the listed operations.
 */
@SuppressWarnings("MissingJavadocMethod")
public class ExistsInExpr extends Expr {

  public SourceLocation loc;

  @Child
  public List<IsId> operations;

  public ExistsInExpr(List<IsId> operations, SourceLocation loc) {
    this.operations = operations;
    this.loc = loc;
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
    builder.append("exists in {");
    var isFirst = true;
    for (IsId operation : operations) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      operation.prettyPrint(0, builder);
    }
    builder.append("}");
  }

  @Override
  public <R> R accept(ExprVisitor<R> visitor) {
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
    ExistsInExpr that = (ExistsInExpr) o;
    return Objects.equals(operations, that.operations);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(operations);
  }
}
