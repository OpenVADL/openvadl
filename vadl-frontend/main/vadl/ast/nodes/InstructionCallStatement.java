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
import javax.annotation.Nullable;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public final class InstructionCallStatement extends Statement {

  @Child
  public IdentifierOrPlaceholder id;
  @Child
  public List<NamedArgument> namedArguments;
  @Child
  public List<Expr> unnamedArguments;
  public SourceLocation loc;

  /**
   * The instruction or pseudo instruction to which it points.
   * Set by the symboltable.
   */
  @Nullable
  public Definition instrDef;

  public InstructionCallStatement(IdentifierOrPlaceholder id, List<NamedArgument> namedArguments,
                           List<Expr> unnamedArguments, SourceLocation loc) {
    this.id = id;
    this.namedArguments = namedArguments;
    this.unnamedArguments = unnamedArguments;
    this.loc = loc;
  }

  public Identifier id() {
    return (Identifier) id;
  }


  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    id.prettyPrint(indent, builder);
    if (!namedArguments.isEmpty()) {
      builder.append("{");
      var isFirst = true;
      for (NamedArgument namedArgument : namedArguments) {
        if (!isFirst) {
          builder.append(", ");
        }
        isFirst = false;
        namedArgument.prettyPrint(indent, builder);
      }
      builder.append("}");
    }
    if (!unnamedArguments.isEmpty()) {
      builder.append("(");
      var isFirst = true;
      for (Expr arg : unnamedArguments) {
        if (!isFirst) {
          builder.append(", ");
        }
        isFirst = false;
        arg.prettyPrint(indent, builder);
      }
      builder.append(")");
    }
    builder.append("\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InstructionCallStatement that = (InstructionCallStatement) o;
    return Objects.equals(id, that.id)
        && Objects.equals(namedArguments, that.namedArguments)
        && Objects.equals(unnamedArguments, that.unnamedArguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, namedArguments, unnamedArguments);
  }

  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visit(this);
  }

  public static final class NamedArgument extends Node {
    @Child
    public IdentifierOrPlaceholder name;
    @Child
    public Expr value;

    public NamedArgument(IdentifierOrPlaceholder name, Expr value) {
      this.name = name;
      this.value = value;
    }

    public Identifier identifier() {
      return (Identifier) name;
    }

    @Override
    public SyntaxType syntaxType() {
      return BasicSyntaxType.INVALID;
    }

    @Override
    public void prettyPrint(int indent, StringBuilder builder) {
      name.prettyPrint(0, builder);
      builder.append(" = ");
      value.prettyPrint(0, builder);
    }

    @Override
    public SourceLocation location() {
      return name.location().join(value.location());
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (NamedArgument) obj;
      return Objects.equals(this.name, that.name)
          && Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, value);
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName();
    }
  }
}
