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
import vadl.types.ConcreteRelationType;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class RelocationDefinition extends Definition implements IdentifiableNode, TypedNode {
  public IdentifierOrPlaceholder identifier;
  public List<Parameter> params;
  public TypeLiteral resultTypeLiteral;
  public Expr expr;
  public SourceLocation loc;

  @Nullable
  public ConcreteRelationType type;

  public RelocationDefinition(IdentifierOrPlaceholder identifier, List<Parameter> params,
                       TypeLiteral resultTypeLiteral,
                       Expr expr, SourceLocation loc) {
    this.identifier = identifier;
    this.params = params;
    this.resultTypeLiteral = resultTypeLiteral;
    this.expr = expr;
    this.loc = loc;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) identifier;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("relocation ");
    identifier.prettyPrint(indent, builder);
    builder.append(" ");
    Parameter.prettyPrintMultiple(indent, params, builder);
    builder.append(" -> ");
    resultTypeLiteral.prettyPrint(0, builder);
    if (isBlockLayout(expr)) {
      builder.append(" =\n");
      expr.prettyPrint(indent + 1, builder);
    } else {
      builder.append(" = ");
      expr.prettyPrint(0, builder);
      builder.append("\n");
    }
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    params.forEach(action);
    action.accept(resultTypeLiteral);
    action.accept(expr);
  }

  @Override
  public <R> R accept(DefinitionVisitor<R> visitor) {
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

    var that = (RelocationDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(identifier, that.identifier)
        && Objects.equals(params, that.params)
        && Objects.equals(resultTypeLiteral, that.resultTypeLiteral)
        && Objects.equals(expr, that.expr);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(params);
    result = 31 * result + Objects.hashCode(resultTypeLiteral);
    result = 31 * result + Objects.hashCode(expr);
    return result;
  }

  @Override
  public ConcreteRelationType type() {
    return requireNonNull(type);
  }
}
