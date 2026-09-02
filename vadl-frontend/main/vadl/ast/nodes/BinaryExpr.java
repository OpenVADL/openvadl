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
import vadl.utils.SourceLocation;

/**
 * Any kind of binary expression (often written with the infix notation in vadl).
 */
@SuppressWarnings("MissingJavadocMethod")
public class BinaryExpr extends Expr {
  public Expr left;
  public IsBinOp operator;
  public Expr right;
  public boolean hasBeenReordered = false;

  public BinaryExpr(Expr left, IsBinOp operator, Expr right) {
    this.left = left;
    this.operator = operator;
    this.right = right;
  }

  public static @Nullable BinaryExpr root = null;

  /**
   * This method reorders a left-sided source expression tree
   * into operator precedence order (as shown in the graph).
   * It mutates the "left" and "right" properties of the expression tree members.
   * <pre>
   *       *            -
   *      / \          / \
   *     *   4   =>   1   *
   *    / \              / \
   *   -   3            *   4
   *  / \              / \
   * 1   2            2   3
   * </pre>
   * Terminology and proof of this algorithm is presented in the article
   * <a href="https://dl.acm.org/doi/pdf/10.1145/357121.357127">by Lalonde and Des Rivieres</a>.
   *
   * @param expr A left-sided binary expression tree.
   * @return the root of the expression tree in operator precedence order
   */
  public static BinaryExpr reorder(BinaryExpr expr) {
    root = expr;
    transformRecRightToLeft(null, expr);
    if (root == null) {
      throw new RuntimeException("Should never happen");
    }
    return root;
  }

  @SuppressWarnings("EnumOrdinal")
  public static BinaryExpr transformRecRightToLeft(@Nullable BinaryExpr parpar, BinaryExpr par) {
    par.hasBeenReordered = true;
    while (par.left instanceof BinaryExpr curr) {
      if (par.operator().precedence.ordinal() > curr.operator().precedence.ordinal()) {
        par.left = curr.right;
        curr.right = par;
        if ((par = parpar) != null) {
          par.left = curr;
          return par;
        }
        root = curr;
      }
      par = transformRecRightToLeft(par, curr);
    }
    return par;
  }

  public Operator operator() {
    return ((BinOp) operator).operator;
  }

  @Override
  public Precedence precedence() {
    return operator().precedence;
  }

  @Override
  public SourceLocation location() {
    return left.location().join(right.location());
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }


  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    wrapInGroup(parentPrec, builder, false, () -> {
      left.prettyPrintExpr(indent, builder, precedence());
      builder.append(" ");
      operator.prettyPrint(0, builder);
      builder.append(" ");
      right.prettyPrintExpr(indent, builder, precedence());
    });
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    action.accept(left);
    action.accept(right);
  }

  @Override
  public <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public String toString() {
    return "%s operator: %s".formatted(this.getClass().getSimpleName(), operator().symbol);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BinaryExpr that = (BinaryExpr) o;
    return Objects.equals(left, that.left) && operator.equals(that.operator)
        && Objects.equals(right, that.right);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(left);
    result = 31 * result + Objects.hashCode(operator);
    result = 31 * result + Objects.hashCode(right);
    return result;
  }
}
