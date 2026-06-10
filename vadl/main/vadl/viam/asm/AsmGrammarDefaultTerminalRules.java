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
 * Core description of default assembly terminal rules.
 */
public final class AsmGrammarDefaultTerminalRules {

  /**
   * Default assembly terminal rule.
   */
  public record TerminalRule(String name, String regularExpression, boolean escapeRegex) {
    Pattern pattern() {
      return Pattern.compile(escapeRegex ? Pattern.quote(regularExpression) : regularExpression);
    }
  }

  /**
   * Default assembly terminal rules in parser priority order.
   */
  public static final List<TerminalRule> RULES = List.of(
      new TerminalRule("IDENTIFIER", "[a-zA-Z_.][a-zA-Z0-9_$.@]*", false),
      new TerminalRule("STRING", "\\\".*\\\"", false),
      new TerminalRule("INTEGER", "0b[01]+|0[0-7]+|[1-9][0-9]*|0x[0-9a-fA-F]+", false),
      new TerminalRule("COLON", ":", false),
      new TerminalRule("PLUS", "+", true),
      new TerminalRule("MINUS", "-", false),
      new TerminalRule("TILDE", "~", false),
      new TerminalRule("SLASH", "/", false),
      new TerminalRule("BACKSLASH", "\\\\", false),
      new TerminalRule("LPAREN", "(", true),
      new TerminalRule("RPAREN", ")", true),
      new TerminalRule("LBRAC", "[", true),
      new TerminalRule("RBRAC", "]", true),
      new TerminalRule("LCURLY", "{", true),
      new TerminalRule("RCURLY", "}", true),
      new TerminalRule("STAR", "*", true),
      new TerminalRule("DOT", ".", true),
      new TerminalRule("COMMA", ",", false),
      new TerminalRule("DOLLAR", "$", true),
      new TerminalRule("EQUAL", "=", false),
      new TerminalRule("EQUALEQUAL", "==", false),
      new TerminalRule("PIPE", "|", true),
      new TerminalRule("PIPEPIPE", "||", true),
      new TerminalRule("CARET", "^", true),
      new TerminalRule("AMP", "&", false),
      new TerminalRule("AMPAMP", "&&", false),
      new TerminalRule("EXCLAIM", "!", false),
      new TerminalRule("EXCLAIMEQUAL", "!=", false),
      new TerminalRule("PERCENT", "%", false),
      new TerminalRule("HASH", "#", false),
      new TerminalRule("LESS", "<", false),
      new TerminalRule("LESSEQUAL", "<=", false),
      new TerminalRule("LESSLESS", "<<", false),
      new TerminalRule("LESSGREATER", "<>", false),
      new TerminalRule("GREATER", ">", false),
      new TerminalRule("GREATEREQUAL", ">=", false),
      new TerminalRule("GREATERGREATER", ">>", false),
      new TerminalRule("AT", "@", false),
      new TerminalRule("MINUSGREATER", "->", false),
      new TerminalRule("EOL", "\\r\\n?|\\n", true)
  );

  private static final List<CompiledRule> COMPILED_RULES = RULES.stream()
      .map(rule -> new CompiledRule(rule.name(), rule.pattern()))
      .toList();

  private AsmGrammarDefaultTerminalRules() {}

  /**
   * Get the name of the terminal rule that matches the given parse value.
   *
   * @param parseValue the parse value to match
   * @return the name of the matching terminal rule, or null if no match is found
   */
  public static @Nullable String getMatchingTerminalRule(String parseValue) {
    for (var rule : COMPILED_RULES) {
      var matcher = rule.pattern().matcher(parseValue);
      if (matcher.matches()) {
        return rule.name();
      }
    }
    return null;
  }

  private record CompiledRule(String name, Pattern pattern) {}
}
