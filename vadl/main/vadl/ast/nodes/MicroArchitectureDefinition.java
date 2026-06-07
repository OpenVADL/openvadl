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

package vadl.ast;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Objects;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings("MissingJavadocType")
public class MicroArchitectureDefinition extends Definition implements IdentifiableNode {
  IdentifierOrPlaceholder id;
  @Child
  IsId isa;
  @Child
  List<Definition> definitions;
  SourceLocation loc;

  MicroArchitectureDefinition(IdentifierOrPlaceholder id, IsId isa, List<Definition> definitions,
                              SourceLocation loc) {
    this.id = id;
    this.isa = isa;
    this.definitions = definitions;
    this.loc = loc;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) id;
  }

  InstructionSetDefinition isaNode() {
    return (InstructionSetDefinition) requireNonNull(isa.target());
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.INVALID;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));
    builder.append("micro architecture ");
    id.prettyPrint(0, builder);
    builder.append(" implements ");
    isa.prettyPrint(0, builder);
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
    MicroArchitectureDefinition that = (MicroArchitectureDefinition) o;
    return Objects.equals(id, that.id)
        && Objects.equals(isa, that.isa)
        && Objects.equals(definitions, that.definitions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, isa, definitions);
  }

}
