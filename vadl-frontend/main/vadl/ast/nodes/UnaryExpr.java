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
import javax.annotation.Nullable;
import vadl.types.BuiltInTable;
import vadl.utils.SourceLocation;

/**
 * A unary expression using prefix notation.
 * Supported operators are {@code -} (negation), {@code !} (logical not),
 * and {@code ~} (bitwise complement).
 */
@SuppressWarnings("MissingJavadocMethod")
public class UnaryExpr extends Expr {
  public IsUnOp operator;
  public Expr operand;

  /**
   * The builtin that will be called.
   * Set by the typechecker.
   */
  @Nullable
  public BuiltInTable.BuiltIn computedTarget;

  public UnaryExpr(IsUnOp operator, Expr operand) {
    this.operator = operator;
    this.operand = operand;
  }

  public UnOp unOp() {
    return (UnOp) operator;
  }

  @Override
  public Precedence precedence() {
    return Precedence.UnaryOp;
  }

  @Override
  public SourceLocation location() {
    return operator.location().join(operand.location());
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    operator.prettyPrint(indent, builder);
    wrapInGroup(parentPrec, builder, true, () -> {
      operand.prettyPrintExpr(indent, builder, precedence());
    });
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    action.accept(operand);
  }

  @Override
  public <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "%s operator: %s".formatted(this.getClass().getSimpleName(), operator);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UnaryExpr that = (UnaryExpr) o;
    return operator.equals(that.operator) && Objects.equals(operand, that.operand);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(operator);
    result = 31 * result + Objects.hashCode(operand);
    return result;
  }
}
