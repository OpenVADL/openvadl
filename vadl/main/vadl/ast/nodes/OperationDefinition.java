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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings("MissingJavadocType")
public class OperationDefinition extends Definition implements IdentifiableNode {
  IdentifierOrPlaceholder name;

  @Child
  List<IsId> resources;

  /**
   * This is the actual set of instructions this operation set contains.
   * The resources might contain other operations, however here they are expaned.
   * Populated by the Typechecker.
   */
  Set<InstructionDefinition> instructions = new HashSet<>();

  SourceLocation loc;

  OperationDefinition(IdentifierOrPlaceholder name, List<IsId> resources, SourceLocation loc) {
    this.name = name;
    this.resources = resources;
    this.loc = loc;
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent));
    builder.append("operation ");
    name.prettyPrint(indent, builder);
    builder.append(" =");
    if (resources.isEmpty()) {
      builder.append(" {}\n");
    } else {
      builder.append("\n");
      var isFirst = true;
      for (IsId resource : resources) {
        builder.append(prettyIndentString(indent));
        builder.append(isFirst ? "{ " : ", ");
        isFirst = false;
        resource.prettyPrint(0, builder);
        builder.append("\n");
      }
      builder.append(prettyIndentString(indent)).append("}\n");
    }
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
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
    OperationDefinition that = (OperationDefinition) o;
    return Objects.equals(name, that.name)
        && Objects.equals(resources, that.resources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, resources);
  }

  @Override
  public Identifier identifier() {
    return (Identifier) name;
  }
}
