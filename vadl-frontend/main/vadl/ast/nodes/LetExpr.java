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
import vadl.types.StructType;
import vadl.types.Type;
import vadl.utils.SourceLocation;

/**
 * A let expression that binds a value to one or more identifiers within a body expression.
 * Written as {@code let x = expr in body}.
 * Supports tuple unpacking when multiple identifiers are provided:
 * {@code let a, b = tupleExpr in body}.
 */
@SuppressWarnings("MissingJavadocMethod")
public class LetExpr extends Expr {
  public List<IdentifierOrPlaceholder> identifiers;
  public Expr valueExpr;
  public Expr body;
  public SourceLocation location;

  public LetExpr(List<IdentifierOrPlaceholder> identifiers, Expr valueExpr, Expr body,
          SourceLocation location) {
    this.identifiers = identifiers;
    this.valueExpr = valueExpr;
    this.body = body;
    this.location = location;
  }

  public List<Identifier> identifiers() {
    return identifiers.stream().map(id -> (Identifier) id).toList();
  }

  /**
   * Translates the outer name of the let expression to the inner name of the value expression.
   * E.g.:
   *
   * <pre>
   *   let next, status = VADL::adds(PC, 4 as Bits<32>) in
   *       ...
   * </pre>
   * this method will translate "next" to "result" and "status" to "status".
   *
   * @param name the bound name of the let expression.
   * @return the name of the value expression.
   */
  public String mapName(String name) {
    var valType = valueExpr.type;
    if (!(valType instanceof StructType struct)) {
      throw new IllegalStateException("Expected StructType but got " + valType);
    }
    final List<String> fields = struct.fieldNames();
    for (var i = 0; i < identifiers.size(); i++) {
      if (name.equals(identifiers().get(i).name)) {
        return fields.get(i);
      }
    }
    throw new IllegalStateException("Let expression does not have a name `%s`.".formatted(name));
  }

  /**
   * Returns the type of one of the variables the expression defines.
   *
   * @return the type of the name provided.
   */
  public Type getTypeOf(String name) {
    var valType = valueExpr.type;
    if (identifiers.size() == 1) {
      return requireNonNull(valType);
    }

    if (!(valType instanceof StructType valStruct)) {
      throw new IllegalStateException("Expected StructType but got " + valType);
    }

    return requireNonNull(valStruct.get(mapName(name)));
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append(prettyIndentString(indent));
    builder.append("(let ");
    var isFirst = true;
    for (var identifier : identifiers) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      identifier.prettyPrint(indent, builder);
    }
    builder.append(" = ");
    valueExpr.prettyPrintExpr(indent + 1, builder, Precedence.NoPrecedence);
    builder.append(" in\n");
    if (!isBlockLayout(body)) {
      builder.append(prettyIndentString(indent + 1));
    }
    body.prettyPrintExpr(indent + 1, builder, Precedence.NoPrecedence);
    builder.append(")");
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    action.accept(valueExpr);
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

    LetExpr that = (LetExpr) o;
    return identifiers.equals(that.identifiers)
        && valueExpr.equals(that.valueExpr)
        && body.equals(that.body);
  }

  @Override
  public int hashCode() {
    int result = identifiers.hashCode();
    result = 31 * result + Objects.hashCode(valueExpr);
    result = 31 * result + Objects.hashCode(body);
    return result;
  }
}
