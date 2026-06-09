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

import java.util.Objects;
import javax.annotation.Nullable;
import vadl.javaannotations.ast.Child;
import vadl.types.asmTypes.AsmType;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class AsmGrammarLocalVarDefinition extends Definition implements IdentifiableNode {
  public Identifier id;
  @Child
  public AsmGrammarLiteralDefinition asmLiteral;
  public SourceLocation loc;

  @Nullable
  public AsmType asmType;

  public AsmGrammarLocalVarDefinition(Identifier id, AsmGrammarLiteralDefinition asmLiteral,
                                      SourceLocation loc) {
    this.id = id;
    this.asmLiteral = asmLiteral;
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
    builder.append("var ");
    id.prettyPrint(0, builder);
    builder.append(" = ");
    asmLiteral.prettyPrint(0, builder);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AsmGrammarLocalVarDefinition that = (AsmGrammarLocalVarDefinition) o;
    return Objects.equals(id, that.id) && Objects.equals(asmLiteral, that.asmLiteral);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, asmLiteral);
  }

  @Override
  public Identifier identifier() {
    return id;
  }
}
