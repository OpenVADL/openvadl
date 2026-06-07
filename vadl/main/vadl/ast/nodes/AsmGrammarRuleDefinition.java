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

import java.util.Objects;
import javax.annotation.Nullable;
import vadl.javaannotations.ast.Child;
import vadl.types.asmTypes.AsmType;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class AsmGrammarRuleDefinition extends Definition implements IdentifiableNode {
  Identifier id;
  @Nullable
  @Child
  AsmGrammarTypeDefinition asmTypeDefinition;
  @Child
  AsmGrammarAlternativesDefinition alternatives;
  SourceLocation loc;

  boolean isTerminalRule = false;
  boolean isBuiltinRule = false;
  @Nullable
  AsmType asmType;

  public AsmGrammarRuleDefinition(Identifier id,
                                  @Nullable AsmGrammarTypeDefinition asmTypeDefinition,
                                  AsmGrammarAlternativesDefinition alternatives,
                                  SourceLocation loc) {
    this.id = id;
    this.asmTypeDefinition = asmTypeDefinition;
    this.alternatives = alternatives;
    this.loc = loc;
  }

  @Override
  <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  <R> R accept(AsmGrammarEntityVisitor<R> visitor) {
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
    id.prettyPrint(indent, builder);
    if (asmTypeDefinition != null) {
      asmTypeDefinition.prettyPrint(indent, builder);
    }
    builder.append(" : ");
    builder.append("\n");

    indent++;
    alternatives.prettyPrint(indent, builder);
    indent--;

    builder.append("\n").append(prettyIndentString(indent)).append(";\n\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AsmGrammarRuleDefinition that = (AsmGrammarRuleDefinition) o;
    return Objects.equals(id, that.id) && Objects.equals(asmTypeDefinition, that.asmTypeDefinition)
        && Objects.equals(alternatives, that.alternatives);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, asmTypeDefinition, alternatives);
  }

  @Override
  public Identifier identifier() {
    return id;
  }
}
