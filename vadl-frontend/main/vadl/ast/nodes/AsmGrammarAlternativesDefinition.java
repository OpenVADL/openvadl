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
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import vadl.types.asmTypes.AsmType;
import vadl.utils.SourceLocation;
import vadl.viam.asm.AsmToken;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class AsmGrammarAlternativesDefinition extends Definition {
  public List<List<AsmGrammarElementDefinition>> alternatives;
  public SourceLocation loc;

  @Nullable
  public AsmType asmType;
  @Nullable
  public List<Set<AsmToken>> alternativesFirstTokens;
  @Nullable
  public Set<AsmToken> enclosingBlockFirstTokens;

  public AsmGrammarAlternativesDefinition(List<List<AsmGrammarElementDefinition>> alternatives,
                                          SourceLocation loc) {
    this.alternatives = alternatives;
    this.loc = loc;
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    // This is too complicated for the @Child annotation
    for (var l : alternatives) {
      for (var a : l) {
        if (a != null) {
          action.accept((Node) a);
        }
      }
    }
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
    var elementIndent = indent + 1;
    for (int i = 0; i < alternatives.size(); i++) {
      var alternative = alternatives.get(i);
      if (i != 0) {
        builder.append(prettyIndentString(indent));
        builder.append("|\n");
      }
      for (int j = 0; j < alternative.size(); j++) {
        var element = alternative.get(j);
        element.prettyPrint(elementIndent, builder);
        if (j != alternative.size() - 1) {
          builder.append("\n");
        }
      }
      if (i != alternatives.size() - 1) {
        builder.append("\n");
      }
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
    AsmGrammarAlternativesDefinition that = (AsmGrammarAlternativesDefinition) o;

    boolean equal = true;
    for (int i = 0; i < alternatives.size(); i++) {
      var curAlternative = alternatives.get(i);
      for (int j = 0; j < curAlternative.size(); j++) {
        try {
          equal &= Objects.equals(curAlternative.get(j), that.alternatives.get(i).get(j));
        } catch (IndexOutOfBoundsException e) {
          return false;
        }
      }
    }
    return equal;
  }

  @Override
  public int hashCode() {
    return Objects.hash(alternatives);
  }
}
