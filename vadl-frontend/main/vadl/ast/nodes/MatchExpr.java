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
import vadl.utils.SourceLocation;

/**
 * A match expression that selects a result based on pattern matching.
 * Written as {@code match expr with { pattern => result, ..., _ => default }}.
 * Each case has one or more patterns on the LHS and a result expression on the RHS of {@code =>}.
 * Must contain a wildcard {@code _} as the default catch-all case.
 */
@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class MatchExpr extends Expr {
  public Expr candidate;
  public List<Case> cases;
  public Expr defaultResult;
  public SourceLocation loc;

  public MatchExpr(Expr candidate, List<Case> cases, Expr defaultResult, SourceLocation loc) {
    this.candidate = candidate;
    this.cases = cases;
    this.defaultResult = defaultResult;
    this.loc = loc;
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
  public void prettyPrintExpr(int indent, StringBuilder builder, Precedence parentPrec) {
    builder.append(prettyIndentString(indent)).append("match ");
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
    builder.append(prettyIndentString(indent + 1)).append(", _ => ");
    defaultResult.prettyPrint(0, builder);
    builder.append("\n").append(prettyIndentString(indent + 1)).append("}\n");
  }

  @Override
  public void forEachChild(Consumer<Node> action) {
    super.forEachChild(action);

    action.accept(candidate);

    for (var c : cases) {
      c.patterns.forEach(action);

      if (c.result != null) {
        action.accept(c.result);
      }
    }

    if (defaultResult != null) {
      action.accept(defaultResult);
    }
  }

  @Override
  public <R> R accept(ExprVisitor<R> visitor) {
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

    MatchExpr that = (MatchExpr) o;
    return Objects.equals(candidate, that.candidate)
        && Objects.equals(cases, that.cases)
        && Objects.equals(defaultResult, that.defaultResult);
  }

  @Override
  public int hashCode() {
    int result = cases.hashCode();
    result = 31 * result + cases.hashCode();
    result = 31 * result + defaultResult.hashCode();
    return result;
  }

  public static class Case {
    public List<Expr> patterns;
    public Expr result;

    public Case(List<Expr> patterns, Expr result) {
      this.patterns = patterns;
      this.result = result;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Case other = (Case) o;
      return patterns.equals(other.patterns) && result.equals(other.result);
    }

    @Override
    public int hashCode() {
      int result1 = patterns.hashCode();
      result1 = 31 * result1 + result.hashCode();
      return result1;
    }
  }
}
