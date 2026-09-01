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

import java.util.function.Consumer;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

/**
 * A sequence call expression used in the ABI modeling view to represent a list of alias
 * registers with the same identifier.
 * Provides syntactic sugar like {@code a{1..4}} which is equivalent to listing
 * {@code a1, a2, a3, a4} individually.
 */
@SuppressWarnings("MissingJavadocMethod")
public class SequenceCallExpr extends Expr {
  public IsCallExpr target;
  @Nullable
  public Expr range;
  public SourceLocation loc;

  public SequenceCallExpr(IsCallExpr target, @Nullable Expr range, SourceLocation loc) {
    this.target = target;
    this.range = range;
    this.loc = loc;
  }

  @Override
  public <R> R accept(ExprVisitor<R> visitor) {
    return visitor.visit(this);
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
    target.prettyPrint(0, builder);
    if (range != null) {
      builder.append("{");
      range.prettyPrint(0, builder);
      builder.append("}");
    }
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    action.accept((Node) target);

    if (range != null) {
      action.accept(range);
    }
  }
}
