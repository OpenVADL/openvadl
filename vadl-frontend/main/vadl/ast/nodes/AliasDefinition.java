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
import vadl.viam.Constant;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class AliasDefinition extends Definition implements IdentifiableNode, TypedNode {
  public IdentifierOrPlaceholder id;
  public AliasKind kind;
  @Nullable
  public TypeLiteral aliasType;
  @Nullable
  public TypeLiteral targetType;
  public Expr value;
  public SourceLocation loc;

  @Nullable
  public Type type;

  /**
   * Set by the typechecker, the register file or register the alias points to.
   */
  @Nullable
  public Definition computedTarget;
  /**
   * Set by the typechecker, the arguments used to pre-access the computedTarget.
   */
  @Nullable
  public List<Expr> computedFixedArgs;

  @Nullable
  public Constant.BitSlice slice;

  public AliasDefinition(IdentifierOrPlaceholder id, AliasKind kind,
                  @Nullable TypeLiteral aliasType, @Nullable TypeLiteral targetType, Expr value,
                  SourceLocation location) {
    this.id = id;
    this.kind = kind;
    this.aliasType = aliasType;
    this.targetType = targetType;
    this.value = value;
    this.loc = location;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) id;
  }

  @Override
  public Type type() {
    return requireNonNull(type);
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
    builder.append(prettyIndentString(indent)).append("alias ");
    switch (kind) {
      case REGISTER -> builder.append("register ");
      case PROGRAM_COUNTER -> builder.append("program counter ");
      default -> {
      }
    }
    id.prettyPrint(0, builder);
    if (aliasType != null) {
      builder.append(" : ");
      aliasType.prettyPrint(0, builder);
      if (targetType != null) {
        builder.append(" -> ");
        targetType.prettyPrint(0, builder);
      }
    }
    builder.append(" = ");
    value.prettyPrint(0, builder);
    builder.append("\n");
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    if (aliasType != null)
      action.accept(aliasType);

    if (targetType != null)
      action.accept(targetType);

    if (value != null)
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

    var that = (AliasDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(id, that.id)
        && Objects.equals(kind, that.kind)
        && Objects.equals(aliasType, that.aliasType)
        && Objects.equals(targetType, that.targetType)
        && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(id);
    result = 31 * result + Objects.hashCode(kind);
    result = 31 * result + Objects.hashCode(aliasType);
    result = 31 * result + Objects.hashCode(targetType);
    result = 31 * result + Objects.hashCode(value);
    return result;
  }

  public enum AliasKind {
    REGISTER, PROGRAM_COUNTER
  }
}
