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

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;
import vadl.types.Type;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod", "EnumOrdinal"})
public abstract class Expr extends Node implements TypedNode {
  @Nullable
  public Type type = null;

  @Override
  public Type type() {
    return requireNonNull(type);
  }

  public Precedence precedence() {
    return Precedence.NoPrecedence;
  }

  public abstract void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec);

  /**
   * Wraps the expression in parentheses depending on this precedence and the
   * parent's one.
   *
   * <p>If weak is true, we don't care about grouping expressions with the same precedence,
   * which is fine for `as` and unary operations.
   * However, when weak is false (i.e. binaryOps),
   * we must group expressions with the same precedences
   * for safety reasons: {@code e.g. a >> b << c != a >> (b << c)}
   *
   * @param parentPrec precedence of parent
   * @param sb         string builder to add parentheses
   * @param weak       if true parentheses are not added when parent has same precedence
   * @param builder    lambda that builds the inner expression printing
   */
  public void wrapInGroup(Precedence parentPrec, StringBuilder sb, boolean weak, Runnable builder) {
    if (parentPrec.ordinal() > precedence().ordinal()
        || (!weak && parentPrec.ordinal() == precedence().ordinal())) {
      sb.append("(");
      builder.run();
      sb.append(")");
    } else {
      builder.run();
    }
  }

  @Override
  public final void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintExpr(indent, builder, Precedence.NoPrecedence);
  }

  public abstract <R> R accept(ExprVisitor<R> visitor);
}
