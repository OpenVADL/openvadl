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
import vadl.types.Type;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class RegisterDefinition extends Definition implements IdentifiableNode, TypedNode {
  public IdentifierOrPlaceholder identifier;
  public RelationTypeLiteral typeLiteral;
  public SourceLocation loc;
  @Nullable
  public Type type;

  public RegisterDefinition(IdentifierOrPlaceholder identifier, RelationTypeLiteral typeLiteral,
                            SourceLocation location) {
    this.identifier = identifier;
    this.typeLiteral = typeLiteral;
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
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("register ");
    identifier.prettyPrint(indent, builder);
    builder.append(": ");
    typeLiteral.prettyPrint(indent, builder);
    builder.append("\n");
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    action.accept(typeLiteral);
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

    RegisterDefinition that = (RegisterDefinition) o;
    return annotations.equals(that.annotations)
        && identifier.equals(that.identifier)
        && typeLiteral.equals(that.typeLiteral);
  }

  @Override
  public int hashCode() {
    int result = annotations.hashCode();
    result = 31 * result + identifier.hashCode();
    result = 31 * result + typeLiteral.hashCode();
    return result;
  }

  @Override
  public Type type() {
    return requireNonNull(type);
  }

  public static final class RelationTypeLiteral extends Node {
    public final List<TypeLiteral> argTypes;
    public TypeLiteral resultType;

    public RelationTypeLiteral(List<TypeLiteral> argTypes, TypeLiteral resultType) {
      this.argTypes = argTypes;
      this.resultType = resultType;
    }

    public List<TypeLiteral> argTypes() {
      return argTypes;
    }

    public TypeLiteral resultType() {
      return resultType;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (RelationTypeLiteral) obj;
      return Objects.equals(this.argTypes, that.argTypes)
          && Objects.equals(this.resultType, that.resultType);
    }

    @Override
    public int hashCode() {
      return Objects.hash(argTypes, resultType);
    }


    @Override
    public SourceLocation location() {
      if (argTypes().isEmpty()) {
        return resultType.location();
      }
      return argTypes.get(0).location().join(resultType.location());
    }

    @Override
    public SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      var isFirst = true;
      for (TypeLiteral argType : argTypes) {
        if (!isFirst) {
          builder.append(" * ");
        }
        isFirst = false;
        argType.prettyPrint(0, builder);
      }
      if (!argTypes.isEmpty()) {
        builder.append(" -> ");
      }
      resultType.prettyPrint(indent, builder);
    }

    @Override
    public void forEachChild(Consumer<Node> action) {
      super.forEachChild(action);

      argTypes.forEach(action);
      action.accept(resultType);
    }
  }
}
