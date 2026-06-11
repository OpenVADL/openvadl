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
import javax.annotation.Nullable;
import vadl.javaannotations.ast.Child;
import vadl.types.ConcreteRelationType;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public final class ExceptionDefinition extends Definition implements IdentifiableNode, TypedNode {
  public IdentifierOrPlaceholder id;
  @Child
  public Statement statement;
  @Child
  public List<Parameter> params;
  public SourceLocation loc;

  @Nullable
  public ConcreteRelationType type;

  public ExceptionDefinition(IdentifierOrPlaceholder id, List<Parameter> params,
                      Statement statement,
                      SourceLocation location) {
    this.id = id;
    this.params = params;
    this.statement = statement;
    this.loc = location;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) id;
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
    builder.append(prettyIndentString(indent)).append("exception ");
    id.prettyPrint(0, builder);
    builder.append(" = ");
    statement.prettyPrint(indent + 1, builder);
    builder.append("\n");
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

    var that = (ExceptionDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(id, that.id)
        && Objects.equals(statement, that.statement)
        && Objects.equals(params, that.params);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(id);
    result = 31 * result + Objects.hashCode(statement);
    result = 31 * result + Objects.hashCode(params);
    return result;
  }

  @Override
  public ConcreteRelationType type() {
    return requireNonNull(type);
  }
}
