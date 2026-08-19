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
import java.util.function.Consumer;
import javax.annotation.Nullable;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class PseudoInstructionDefinition extends InstructionSequenceDefinition
    implements IdentifiableNode {
  public IdentifierOrPlaceholder identifier;
  public PseudoInstrKind kind;

  /**
   * The matching assembly definition.
   * Set by the symboltable.
   */
  @Nullable
  public AssemblyDefinition assemblyDefinition;

  public PseudoInstructionDefinition(IdentifierOrPlaceholder identifier, PseudoInstrKind kind,
                                     List<Parameter> params,
                                     List<InstructionSequenceStatement> statements,
                                     SourceLocation loc) {
    super(params, statements, loc);
    this.identifier = identifier;
    this.kind = kind;
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    // Since this class has no @Child annotations the Annotationprocessor doesn't find it.
    NodeChildrenRegistry.unsafeForEachChildDirect(this,
        (Class<? extends Node>) getClass().getSuperclass(), action);
  }

  @Override
  public Identifier identifier() {
    return (Identifier) identifier;
  }


  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.ISA_DEFS;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    prettyPrintAnnotations(indent, builder);
    builder.append(prettyIndentString(indent));
    var kindStr = switch (kind) {
      case PSEUDO -> "pseudo";
      case COMPILER -> "compiler";
    };
    builder.append(kindStr).append(" instruction ");
    identifier.prettyPrint(indent, builder);
    Parameter.prettyPrintMultiple(indent, params, builder);
    builder.append(" = {\n");
    for (InstructionSequenceStatement statement : statements) {
      statement.prettyPrint(indent + 1, builder);
    }
    builder.append(prettyIndentString(indent)).append("}\n");
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

    var that = (PseudoInstructionDefinition) o;
    return Objects.equals(annotations, that.annotations)
        && Objects.equals(identifier, that.identifier)
        && Objects.equals(kind, that.kind)
        && Objects.equals(params, that.params)
        && Objects.equals(statements, that.statements);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(annotations);
    result = 31 * result + Objects.hashCode(identifier);
    result = 31 * result + Objects.hashCode(kind);
    result = 31 * result + Objects.hashCode(params);
    result = 31 * result + Objects.hashCode(statements);
    return result;
  }

  public enum PseudoInstrKind {
    PSEUDO, COMPILER
  }
}
