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

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import vadl.types.BuiltInTable;
import vadl.utils.SourceLocation;

/**
 * A expression to express tensor operations.
 * forall in tesnor
 * forall in fold
 */
@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class ForallExpr extends Expr {
  public List<ForallIndex> indices;

  /**
   * The kind of forall expression (fold, tensor, etc).
   */
  public Operation operation;

  /**
   * Only if the node is a fold we need to know which operator is folded over.
   * The fold can either be a binary operator or a function name.
   */
  @Nullable
  public Node foldAction;

  /// The function beeing called by the fold.
  /// The function must be a built-in and satisfy the type contract `(T, T) -> T`
  /// Set by the typechecker.
  @Nullable
  public BuiltInTable.BuiltIn computedFoldBuiltin;

  public Expr body;

  public SourceLocation loc;

  public ForallExpr(List<ForallIndex> indices, Operation operation, @Nullable Node foldAction,
             Expr body, SourceLocation loc) {
    this.indices = indices;
    this.operation = operation;
    this.foldAction = foldAction;
    this.body = body;
    this.loc = loc;
  }

  public Operator getFoldOperator() {
    return requireNonNull(((BinOp) foldAction)).operator;
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
    builder.append("forall ");
    var isFirst = true;
    for (ForallIndex index : indices) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      index.prettyPrint(indent, builder);
    }
    builder.append(" ").append(operation.keyword);
    if (foldAction != null) {
      builder.append(" ");
      foldAction.prettyPrint(indent, builder);
      builder.append(" with");
    }
    if (isBlockLayout(body)) {
      builder.append("\n");
      body.prettyPrint(indent + 1, builder);
    } else {
      builder.append(" ");
      body.prettyPrint(0, builder);
      builder.append("\n");
    }
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    indices.forEach(action);

    if (foldAction != null)
      action.accept(foldAction);

    action.accept(body);
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
    ForallExpr that = (ForallExpr) o;
    return Objects.equals(indices, that.indices) && operation == that.operation
        && Objects.equals(foldAction, that.foldAction) && Objects.equals(body, that.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(indices, operation, foldAction, body);
  }

  @Override
  public String toString() {
    return "%s keyword: %s, type: %s".formatted(getClass().getSimpleName(), operation.keyword,
        type);
  }


  public enum Operation {
    TENSOR("tensor"), FOLD("fold");

    public final String keyword;

    Operation(String keyword) {
      this.keyword = keyword;
    }
  }
}
