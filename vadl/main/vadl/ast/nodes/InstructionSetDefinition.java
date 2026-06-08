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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class InstructionSetDefinition extends Definition implements IdentifiableNode {
  public IdentifierOrPlaceholder identifier;
  @Child
  public List<IsId> extending;
  @Child
  public List<Definition> definitions;
  public SourceLocation loc;

  public InstructionSetDefinition(IdentifierOrPlaceholder identifier,
                           List<IsId> extending,
                           List<Definition> statements, SourceLocation location) {
    this.identifier = identifier;
    this.extending = extending;
    this.definitions = statements;
    this.loc = location;
  }

  @Override
  public Identifier identifier() {
    return (Identifier) identifier;
  }

  public List<InstructionSetDefinition> extendingNodes() {
    return extending.stream()
        .map(id -> (InstructionSetDefinition) requireNonNull(id.target()))
        .toList();
  }

  /**
   * Get all nodes of the given type, possibly inherited by a base ISA.
   *
   * @param type The node type.
   * @param <T>  The generic node type.
   * @return The stream of nodes.
   */
  public <T extends Node> Stream<T> allInheritedNodesOf(Class<T> type) {
    return Stream.concat(extendingNodes().stream()
            .flatMap(isa -> isa.allInheritedNodesOf(type)),
        definitions.stream()
            .filter(type::isInstance)
            .map(type::cast));
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
    builder.append(prettyIndentString(indent));
    builder.append("instruction set architecture ").append(identifier().name);
    if (!extending.isEmpty()) {
      var extStr = extending.stream().map(IsId::pathToString).collect(Collectors.joining(", "));
      builder.append(" extending ").append(extStr);
    }
    builder.append(" = {\n");
    prettyPrintDefinitions(indent + 1, builder, definitions);
    builder.append("}\n");
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

    var that = (InstructionSetDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(identifier, that.identifier)
        && Objects.equals(extending, that.extending)
        && Objects.equals(definitions, that.definitions);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(extending);
    result = 31 * result + Objects.hashCode(definitions);
    return result;
  }
}
