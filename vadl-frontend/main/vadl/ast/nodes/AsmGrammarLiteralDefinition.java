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
import vadl.types.asmTypes.AsmType;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class AsmGrammarLiteralDefinition extends Definition {
  @Nullable
  public Identifier id;
  @Child
  public List<AsmGrammarLiteralDefinition> parameters;
  @Nullable
  @Child
  public Expr stringLiteral;
  @Nullable
  @Child
  public AsmGrammarTypeDefinition asmTypeDefinition;
  public SourceLocation loc;

  @Nullable
  public AsmType asmType;

  public AsmGrammarLiteralDefinition(@Nullable Identifier id,
                                     List<AsmGrammarLiteralDefinition> parameters,
                                     @Nullable Expr stringLiteral, @Nullable
                                     AsmGrammarTypeDefinition asmTypeDefinition,
                                     SourceLocation loc) {
    this.id = id;
    this.parameters = parameters;
    this.stringLiteral = stringLiteral;
    this.asmTypeDefinition = asmTypeDefinition;
    this.loc = loc;
  }

  @Override
  public <R> R accept(DefinitionVisitor<R> visitor) {
    return visitor.visit(this);
  }

  public <R> R accept(AsmGrammarEntityVisitor<R> visitor) {
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
    builder.append(prettyIndentString(indent));
    if (id != null) {
      id.prettyPrint(0, builder);
      if (!parameters.isEmpty()) {
        builder.append('<');
        for (int i = 0; i < parameters.size(); i++) {
          parameters.get(i).prettyPrint(indent, builder);
          if (i != parameters.size() - 1) {
            builder.append(", ");
          }
        }
        builder.append('>');
      }
    }
    if (stringLiteral != null) {
      stringLiteral.prettyPrint(0, builder);
    }
    if (asmTypeDefinition != null) {
      asmTypeDefinition.prettyPrint(0, builder);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AsmGrammarLiteralDefinition that = (AsmGrammarLiteralDefinition) o;
    return Objects.equals(id, that.id) && Objects.equals(stringLiteral, that.stringLiteral)
        && Objects.equals(asmTypeDefinition, that.asmTypeDefinition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, stringLiteral, asmTypeDefinition);
  }
}
