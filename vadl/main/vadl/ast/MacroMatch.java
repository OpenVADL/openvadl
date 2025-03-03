// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
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

import java.util.List;
import vadl.utils.SourceLocation;

record MacroMatch(SyntaxType resultType, List<Choice> choices, Node defaultChoice,
                  SourceLocation sourceLocation) {

  void prettyPrint(int indent, StringBuilder sb) {
    sb.append("  ".repeat(indent)).append("match : ").append(resultType.print()).append("(");
    for (Choice choice : choices) {
      sb.append("\n").append("  ".repeat(indent + 1));
      var isFirst = true;
      for (Pattern pattern : choice.patterns) {
        if (!isFirst) {
          sb.append(", ");
        }
        isFirst = false;
        pattern.candidate.prettyPrint(indent + 1, sb);
        if (pattern.comparison == Comparison.EQUAL) {
          sb.append(" = ");
        } else {
          sb.append(" != ");
        }
        pattern.match.prettyPrint(indent + 1, sb);
      }
      sb.append(" => ");
      choice.result.prettyPrint(indent + 1, sb);
      sb.append(" ;");
    }
    sb.append("\n").append(" ".repeat(indent + 1));
    sb.append("_ => ");
    defaultChoice.prettyPrint(indent + 1, sb);
    sb.append("\n").append(" ".repeat(indent)).append(")\n");
  }

  enum Comparison { EQUAL, NOT_EQUAL }

  record Choice(List<Pattern> patterns, Node result) {
  }

  record Pattern(Node candidate, Comparison comparison, Node match) {
  }
}

sealed interface IsMacroMatch
    permits MacroMatchNode, MacroMatchExpr, MacroMatchStatement, MacroMatchDefinition {
}
