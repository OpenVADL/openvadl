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
import java.util.function.Consumer;
import vadl.utils.SourceLocation;

/**
 * An existential quantifier expression with a body.
 * Written as {@code exists x in {op1, op2, ...} then expr}.
 * Binds identifiers to operations from the given set and evaluates the body expression.
 */
@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class ExistsInThenExpr extends Expr {
  public List<ExistsInThenExpr.Index> indices;
  public Expr thenExpr;
  public SourceLocation loc;

  public ExistsInThenExpr(List<ExistsInThenExpr.Index> indices, Expr thenExpr, SourceLocation loc) {
    this.indices = indices;
    this.thenExpr = thenExpr;
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
    builder.append("exists ");
    var isFirst = true;
    for (ExistsInThenExpr.Index index : indices) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      index.prettyPrint(indent, builder);
    }
    if (isBlockLayout(thenExpr)) {
      builder.append(" then\n");
      thenExpr.prettyPrint(indent + 1, builder);
    } else {
      builder.append(" then ");
      thenExpr.prettyPrint(0, builder);
      builder.append("\n");
    }
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    indices.forEach(action);
    action.accept(thenExpr);
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
    ExistsInThenExpr that = (ExistsInThenExpr) o;
    return Objects.equals(indices, that.indices)
        && Objects.equals(thenExpr, that.thenExpr);
  }

  @Override
  public int hashCode() {
    return Objects.hash(indices, thenExpr);
  }

  public static final class Index extends Node implements IdentifiableNode {
    public IsId id;
    public List<IsId> operations;

    public Index(IsId id, List<IsId> operations) {
      this.id = id;
      this.operations = operations;
    }

    @Override
    public Identifier identifier() {
      return (Identifier) id;
    }

    @Override
    public SourceLocation location() {
      var loc = id.location();
      if (!operations.isEmpty()) {
        loc = loc.join(operations.get(operations.size() - 1).location());
      }
      return loc;
    }

    @Override
    public SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      id.prettyPrint(0, builder);
      builder.append(" in {");
      var isFirstOp = true;
      for (IsId operation : operations) {
        if (!isFirstOp) {
          builder.append(", ");
        }
        isFirstOp = false;
        operation.prettyPrint(0, builder);
      }
      builder.append("}");
    }

    @Override
    public void forEachChild(Consumer<Node> action) {
      super.forEachChild(action);

      operations.forEach(operation -> action.accept((Node) operation));
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Index index = (Index) o;
      return id.equals(index.id) && operations.equals(index.operations);
    }

    @Override
    public int hashCode() {
      int result = id.hashCode();
      result = 31 * result + operations.hashCode();
      return result;
    }
  }
}
