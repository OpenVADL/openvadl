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
public class ApplicationBinaryInterfaceDefinition extends Definition implements IdentifiableNode {
  public IdentifierOrPlaceholder id;
  @Child
  public IsId isa;
  @Child
  public List<Definition> definitions;
  public SourceLocation loc;

  @Nullable
  public InstructionSetDefinition isaNode;

  public ApplicationBinaryInterfaceDefinition(IdentifierOrPlaceholder id,
                                       IsId isa,
                                       List<Definition> definitions,
                                       SourceLocation loc) {
    this.id = id;
    this.isa = isa;
    this.definitions = definitions;
    this.loc = loc;
  }

  @Override
  public <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent)).append("application binary interface ");
    id.prettyPrint(indent, builder);
    builder.append(" for ");
    isa.prettyPrint(indent, builder);
    builder.append(" = {\n");
    prettyPrintDefinitions(indent + 1, builder, definitions);
    builder.append(prettyIndentString(indent)).append("}\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApplicationBinaryInterfaceDefinition that = (ApplicationBinaryInterfaceDefinition) o;
    return Objects.equals(id, that.id) && Objects.equals(isa, that.isa)
        && Objects.equals(definitions, that.definitions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, isa, definitions);
  }

  @Override
  public Identifier identifier() {
    return (Identifier) id;
  }
}
