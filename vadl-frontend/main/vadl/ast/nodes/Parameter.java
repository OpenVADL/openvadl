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
import java.util.function.Consumer;
import vadl.types.Type;
import vadl.utils.SourceLocation;

/**
 * A common parameter type that corresponds to the {@code parameter} grammar rule.
 *
 * <p>name The declared name of this parameter.
 * type The declared type of this parameter.
 */
@SuppressWarnings("MissingJavadocMethod")
public class Parameter extends Definition implements IdentifiableNode, TypedNode {
  public IdentifierOrPlaceholder name;
  public TypeLiteral typeLiteral;

  public Parameter(IdentifierOrPlaceholder name, TypeLiteral typeLiteral) {
    this.name = name;
    this.typeLiteral = typeLiteral;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) name;
  }

  @Override
  public SourceLocation location() {
    return name.location().join(typeLiteral.location());
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    name.prettyPrint(indent, builder);
    builder.append(" : ");
    typeLiteral.prettyPrint(indent, builder);
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    action.accept(typeLiteral);
  }

  public static void prettyPrintMultiple(int indent, List<Parameter> parameters,
                                  StringBuilder builder) {
    if (parameters.isEmpty()) {
      return;
    }

    builder.append("(");
    prettyPrintJoin(", ", parameters, indent, builder);
    builder.append(")");
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Parameter parameter = (Parameter) o;
    return name.equals(parameter.name)
        && typeLiteral.equals(parameter.typeLiteral);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + typeLiteral.hashCode();
    return result;
  }

  @Override
  public Type type() {
    return typeLiteral.type();
  }

  @Override
  public <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
