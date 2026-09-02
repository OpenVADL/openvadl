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
import vadl.types.StructType;
import vadl.types.Type;
import vadl.utils.SourceLocation;

/**
 * If multiple identifiers are provided, they are used to unpack fields of a struct.
 */
@SuppressWarnings("MissingJavadocMethod")
public final class LetStatement extends Statement {
  public List<IsId> identifiers;
  public Expr valueExpr;
  public Statement body;
  public SourceLocation location;

  public LetStatement(List<IsId> identifiers, Expr valueExpr, Statement body,
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
   * Translates the outer name of the let statement to the inner name of the value expression.
   * E.g.:
   *
   * <pre>
   *   let next, status = VADL::adds(PC, 4 as Bits<32>) in
   *       ...
   * </pre>
   * this method will translate "next" to "result" and "status" to "status".
   *
   * @param name the bound name of the let statement.
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
    throw new IllegalStateException("Let statement does not have a name `%s`.".formatted(name));
  }

  /**
   * Returns the type of one of the variables the statement defines.
   *
   * @return the type of the name provided.
   */
  public Type getTypeOf(String name) {
    var valType = valueExpr.type;
    if (identifiers.size() == 1) {
      return Objects.requireNonNull(valType);
    }

    if (!(valType instanceof StructType valStruct)) {
      throw new IllegalStateException("Expected StructType but got " + valType);
    }

    return Objects.requireNonNull(valStruct.fields().get(mapName(name)));
  }

  @Override
  public SourceLocation location() {
    return location;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("let ");
    var isFirst = true;
    for (var identifier : identifiers) {
      if (!isFirst) {
        builder.append(", ");
      }
      isFirst = false;
      identifier.prettyPrint(indent, builder);
    }
    builder.append(" = ");
    valueExpr.prettyPrint(indent + 1, builder);
    builder.append(" in\n");
    body.prettyPrint(indent + 1, builder);
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    action.accept(valueExpr);
    action.accept(body);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (LetStatement) obj;
    return Objects.equals(this.identifiers, that.identifiers)
        && Objects.equals(this.valueExpr, that.valueExpr)
        && Objects.equals(this.body, that.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifiers, valueExpr, body);
  }

  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
