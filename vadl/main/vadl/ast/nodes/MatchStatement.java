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
import vadl.utils.WithLocation;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public final class MatchStatement extends Statement {
  public Expr candidate;
  public List<Case> cases;
  @Nullable
  public Statement defaultResult;
  public SourceLocation loc;

  public MatchStatement(Expr candidate, List<Case> cases, @Nullable Statement defaultResult,
                 SourceLocation loc) {
    this.candidate = candidate;
    this.cases = cases;
    this.defaultResult = defaultResult;
    this.loc = loc;
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    // This is too complicated for the @Child annotation
    action.accept(candidate);
    for (var c : cases) {
      c.patterns.forEach(p -> action.accept(p));
      if (c.result != null) {
        action.accept(c.result);
      }
    }
    if (defaultResult != null) {
      action.accept(defaultResult);
    }
  }

  @Override
  public SourceLocation location() {
    return loc;
  }

  @Override
  public SyntaxType syntaxType() {
    return BasicSyntaxType.EX;
  }

  @Override
  public void prettyPrint(int indent, StringBuilder builder) {
    builder.append("match ");
    candidate.prettyPrint(0, builder);
    builder.append(" with\n");
    builder.append(prettyIndentString(indent + 1)).append("{ ");
    var isFirst = true;
    for (var matchCase : cases) {
      if (!isFirst) {
        builder.append(prettyIndentString(indent + 1)).append(", ");
      }
      isFirst = false;
      if (matchCase.patterns.size() == 1) {
        matchCase.patterns.get(0).prettyPrint(0, builder);
      } else {
        builder.append("{");
        var isFirstPattern = true;
        for (var pattern : matchCase.patterns) {
          if (!isFirstPattern) {
            builder.append(", ");
          }
          isFirstPattern = false;
          pattern.prettyPrint(0, builder);
        }
        builder.append("}");
      }
      builder.append(" => ");
      matchCase.result.prettyPrint(0, builder);
      builder.append("\n");
    }
    if (defaultResult != null) {
      builder.append(prettyIndentString(indent + 1)).append(", _ => ");
      defaultResult.prettyPrint(0, builder);
      builder.append("\n");
    }
    builder.append(prettyIndentString(indent + 1)).append("}\n");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MatchStatement that = (MatchStatement) o;
    return Objects.equals(candidate, that.candidate)
        && Objects.equals(cases, that.cases)
        && Objects.equals(defaultResult, that.defaultResult);
  }

  @Override
  public int hashCode() {
    int result = cases.hashCode();
    result = 31 * result + cases.hashCode();
    result = 31 * result + Objects.hashCode(defaultResult);
    return result;
  }

  public static final class Case implements WithLocation {
    public List<Expr> patterns;
    public Statement result;

    public Case(List<Expr> patterns, Statement result) {
      this.patterns = patterns;
      this.result = result;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (Case) obj;
      return Objects.equals(this.patterns, that.patterns)
          && Objects.equals(this.result, that.result);
    }

    @Override
    public int hashCode() {
      return Objects.hash(patterns, result);
    }

    @Override
    public String toString() {
      return this.getClass().getSimpleName();
    }

    @Override
    public SourceLocation location() {
      return patterns.get(0).location().join(result.location());
    }
  }

  @Override
  public <R> R accept(StatementVisitor<R> visitor) {
    return visitor.visit(this);
  }
}
