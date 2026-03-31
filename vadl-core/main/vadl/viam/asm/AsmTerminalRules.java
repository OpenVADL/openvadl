// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.viam.asm;

import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Terminal token rules shared between frontend asm grammar defaults and VIAM asm parsing.
 */
public final class AsmTerminalRules {
  private AsmTerminalRules() {
  }

  private record TerminalRule(String name, Pattern pattern) {
  }

  private static final List<TerminalRule> DEFAULT_TERMINALS = List.of(
      terminal("IDENTIFIER", "[a-zA-Z_.][a-zA-Z0-9_$.@]*", false),
      terminal("STRING", "\\\".*\\\"", false),
      terminal("INTEGER", "0b[01]+|0[0-7]+|[1-9][0-9]*|0x[0-9a-fA-F]+", false),
      terminal("COLON", ":", false),
      terminal("PLUS", "+", true),
      terminal("MINUS", "-", false),
      terminal("TILDE", "~", false),
      terminal("SLASH", "/", false),
      terminal("BACKSLASH", "\\\\", true),
      terminal("LPAREN", "(", true),
      terminal("RPAREN", ")", true),
      terminal("LBRAC", "[", true),
      terminal("RBRAC", "]", true),
      terminal("LCURLY", "{", true),
      terminal("RCURLY", "}", true),
      terminal("STAR", "*", true),
      terminal("DOT", ".", true),
      terminal("COMMA", ",", false),
      terminal("DOLLAR", "$", true),
      terminal("EQUAL", "=", false),
      terminal("EQUALEQUAL", "==", false),
      terminal("PIPE", "|", true),
      terminal("PIPEPIPE", "||", true),
      terminal("CARET", "^", true),
      terminal("AMP", "&", false),
      terminal("AMPAMP", "&&", false),
      terminal("EXCLAIM", "!", false),
      terminal("EXCLAIMEQUAL", "!=", false),
      terminal("PERCENT", "%", false),
      terminal("HASH", "#", false),
      terminal("LESS", "<", false),
      terminal("LESSEQUAL", "<=", false),
      terminal("LESSLESS", "<<", false),
      terminal("LESSGREATER", "<>", false),
      terminal("GREATER", ">", false),
      terminal("GREATEREQUAL", ">=", false),
      terminal("GREATERGREATER", ">>", false),
      terminal("AT", "@", false),
      terminal("MINUSGREATER", "->", false),
      terminal("EOL", "\\r\\n?|\\n", false)
  );

  private static TerminalRule terminal(String name, String regularExpression,
                                       boolean escapeRegex) {
    var pattern = Pattern.compile(
        escapeRegex ? Pattern.quote(regularExpression) : regularExpression);
    return new TerminalRule(name, pattern);
  }

  /**
   * Get the name of the terminal rule that matches the given parse value.
   */
  public static @Nullable String getMatchingTerminalRule(String parseValue) {
    for (var rule : DEFAULT_TERMINALS) {
      if (rule.pattern.matcher(parseValue).matches()) {
        return rule.name;
      }
    }
    return null;
  }
}
