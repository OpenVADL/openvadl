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
import vadl.types.Type;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class ConstantDefinition extends Definition implements IdentifiableNode, TypedNode {
  public IdentifierOrPlaceholder identifier;

  @Nullable
  public TypeLiteral typeLiteral;

  public Expr value;
  public SourceLocation loc;

  /**
   * Set by the typechecker, the actual value that will be used here.
   */
  @Nullable
  public Object evaluatedValue;

  public ConstantDefinition(IdentifierOrPlaceholder identifier, @Nullable TypeLiteral typeLiteral,
                     Expr value,
                     SourceLocation location) {
    this.identifier = identifier;
    this.typeLiteral = typeLiteral;
    this.value = value;
    this.loc = location;
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
    return BasicSyntaxType.COMMON_DEFS;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("constant %s".formatted(identifier().name));
    if (typeLiteral != null) {
      builder.append(": ");
      typeLiteral.prettyPrint(indent, builder);
    }
    if (isBlockLayout(value)) {
      builder.append(" =\n");
    } else {
      builder.append(" = ");
    }
    value.prettyPrint(indent + 1, builder);
    builder.append("\n");
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    if (typeLiteral != null)
      action.accept(typeLiteral);

    action.accept(value);
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

    ConstantDefinition that = (ConstantDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(identifier, that.identifier)
        && Objects.equals(typeLiteral, that.typeLiteral)
        && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(typeLiteral);
    result = 31 * result + Objects.hashCode(value);
    return result;
  }

  @Override
  public Type type() {
    return value.type();
  }
}
