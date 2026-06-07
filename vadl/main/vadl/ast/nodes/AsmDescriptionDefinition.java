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
import vadl.javaannotations.ast.Child;
import vadl.utils.SourceLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class AsmDescriptionDefinition extends Definition implements IdentifiableNode {
  Identifier id;
  @Child
  Identifier abi;
  @Child
  List<AsmModifierDefinition> modifiers;
  @Child
  List<AsmDirectiveDefinition> directives;
  @Child
  List<AsmGrammarRuleDefinition> rules;
  @Child
  List<Definition> commonDefinitions;
  SourceLocation loc;

  public AsmDescriptionDefinition(Identifier id, Identifier abi,
                                  List<AsmModifierDefinition> modifiers,
                                  List<AsmDirectiveDefinition> directives,
                                  List<AsmGrammarRuleDefinition> rules,
                                  List<Definition> commonDefinitions,
                                  SourceLocation loc) {
    this.id = id;
    this.abi = abi;
    this.modifiers = modifiers;
    this.directives = directives;
    this.rules = rules;
    this.commonDefinitions = commonDefinitions;
    this.loc = loc;
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
    prettyPrintAnnotations(indent, builder);

    builder.append(prettyIndentString(indent));
    builder.append("assembly description ");
    id.prettyPrint(indent, builder);
    builder.append(" for ");
    abi.prettyPrint(indent, builder);
    builder.append(" = {\n");
    indent++;

    if (!commonDefinitions.isEmpty()) {
      prettyPrintDefinitions(indent, builder, commonDefinitions);
    }

    if (!modifiers.isEmpty()) {
      builder.append(prettyIndentString(indent)).append("modifiers = {\n");
      indent++;
      for (var mod : modifiers) {
        mod.prettyPrint(indent, builder);
        if (!Objects.equals(modifiers.get(modifiers.size() - 1), mod)) {
          builder.append(',');
        }
        builder.append("\n");
      }
      builder.append(prettyIndentString(--indent)).append("}\n\n");
    }

    if (!directives.isEmpty()) {
      builder.append(prettyIndentString(indent)).append("directives = {\n\n");
      indent++;
      for (var dir : directives) {
        dir.prettyPrint(indent, builder);
        if (!Objects.equals(directives.get(directives.size() - 1), dir)) {
          builder.append(',');
        }
        builder.append("\n");
      }
      builder.append(prettyIndentString(--indent)).append("}\n");
    }

    builder.append(prettyIndentString(indent)).append("grammar = {\n");
    indent++;
    var printableRules = rules.stream().filter(r -> !r.isBuiltinRule && !r.isTerminalRule).toList();
    for (var rule : printableRules) {
      builder.append(prettyIndentString(indent));
      rule.prettyPrint(indent, builder);
    }
    builder.append(prettyIndentString(--indent)).append("}\n");

    builder.append(prettyIndentString(--indent)).append("}\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AsmDescriptionDefinition that = (AsmDescriptionDefinition) o;
    return Objects.equals(id, that.id) && Objects.equals(abi, that.abi)
        && new HashSet<>(that.rules).containsAll(rules)
        && new HashSet<>(rules).containsAll(that.rules);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, abi, rules);
  }

  @Override
  public Identifier identifier() {
    return id;
  }
}
