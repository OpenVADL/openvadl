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
public class AsmGrammarElementDefinition extends Definition {
  @Nullable
  @Child
  AsmGrammarLocalVarDefinition localVar;
  @Nullable
  @Child
  Identifier attribute;
  Boolean isPlusEqualsAttributeAssign;
  Boolean isAttributeLocalVar = false;
  @Nullable
  @Child
  AsmGrammarLiteralDefinition asmLiteral;
  @Nullable
  @Child
  AsmGrammarAlternativesDefinition groupAlternatives;
  @Nullable
  @Child
  AsmGrammarAlternativesDefinition optionAlternatives;
  @Nullable
  @Child
  AsmGrammarAlternativesDefinition repetitionAlternatives;
  @Nullable
  @Child
  Expr semanticPredicate;
  @Nullable
  @Child
  AsmGrammarTypeDefinition groupAsmTypeDefinition;
  SourceLocation loc;

  @Nullable
  AsmType asmType;
  Boolean isWithinRepetitionBlock = false;

  public AsmGrammarElementDefinition(@Nullable AsmGrammarLocalVarDefinition localVar,
                                     @Nullable Identifier attribute,
                                     Boolean isPlusEqualsAttributeAssign,
                                     @Nullable AsmGrammarLiteralDefinition asmLiteral,
                                     @Nullable AsmGrammarAlternativesDefinition groupAlternatives,
                                     @Nullable AsmGrammarAlternativesDefinition optionAlternatives,
                                     @Nullable
                                     AsmGrammarAlternativesDefinition repetitionAlternatives,
                                     @Nullable Expr semanticPredicate,
                                     @Nullable AsmGrammarTypeDefinition groupAsmType,
                                     SourceLocation loc) {
    this.localVar = localVar;
    this.attribute = attribute;
    this.isPlusEqualsAttributeAssign = isPlusEqualsAttributeAssign;
    this.asmLiteral = asmLiteral;
    this.groupAlternatives = groupAlternatives;
    this.optionAlternatives = optionAlternatives;
    this.repetitionAlternatives = repetitionAlternatives;
    this.semanticPredicate = semanticPredicate;
    this.groupAsmTypeDefinition = groupAsmType;
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

  String symbol() {
    return isPlusEqualsAttributeAssign ? "+=" : "=";
  }

  private void prettyPrintAlternatives(int indent, StringBuilder builder,
                                       @Nullable AsmGrammarAlternativesDefinition alternatives,
                                       char blockStartSymbol, char blockEndSymbol) {
    if (alternatives != null) {
      builder.append(blockStartSymbol).append("\n");
      alternatives.prettyPrint(++indent, builder);
      builder.append(prettyIndentString(--indent));
      builder.append("\n").append(prettyIndentString(indent)).append(blockEndSymbol);
    }
  }

  @Override
  void prettyPrint(int indent, StringBuilder builder) {
    builder.append(prettyIndentString(indent));

    if (localVar != null) {
      localVar.prettyPrint(indent, builder);
    }
    if (attribute != null) {
      attribute.prettyPrint(indent, builder);
      builder.append(" ").append(symbol()).append(" ");
    }
    if (asmLiteral != null) {
      asmLiteral.prettyPrint(0, builder);
    }

    prettyPrintAlternatives(indent, builder, groupAlternatives, '(', ')');
    prettyPrintAlternatives(indent, builder, optionAlternatives, '[', ']');
    prettyPrintAlternatives(indent, builder, repetitionAlternatives, '{', '}');

    if (semanticPredicate != null) {
      builder.append("?( ");
      semanticPredicate.prettyPrint(0, builder);
      builder.append(" )");
    }
    if (groupAsmTypeDefinition != null) {
      groupAsmTypeDefinition.prettyPrint(0, builder);
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
    AsmGrammarElementDefinition that = (AsmGrammarElementDefinition) o;
    return Objects.equals(localVar, that.localVar) && Objects.equals(attribute, that.attribute)
        && Objects.equals(isPlusEqualsAttributeAssign, that.isPlusEqualsAttributeAssign)
        && Objects.equals(asmLiteral, that.asmLiteral)
        && Objects.equals(groupAlternatives, that.groupAlternatives)
        && Objects.equals(groupAsmTypeDefinition, that.groupAsmTypeDefinition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(localVar, attribute, isPlusEqualsAttributeAssign, asmLiteral,
        groupAlternatives, groupAsmTypeDefinition);
  }
}
