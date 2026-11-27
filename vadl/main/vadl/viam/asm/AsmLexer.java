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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Lexer to derive a list of {@link AsmToken} for the {@code input} string
 * given in the constructor. Tokens are derived based on regular expressions
 * defined in {@link AsmGrammarDefaultTerminalRules}.
 */
public class AsmLexer {
  private final String input;
  private int currentPosition = 0;

  public AsmLexer(String input) {
    this.input = input;
  }

  /**
   * Derive a list of {@link AsmToken} for the {@code input}
   * given in the constructor.
   *
   * @return List of the derived {@link AsmToken}
   *         or {@code null} if input contains a character for which no token can be derived
   */
  @Nullable
  public List<AsmToken> tokenize() {
    List<AsmToken> tokens = new ArrayList<>();

    while (currentPosition < input.length()) {
      char currentChar = input.charAt(currentPosition);

      if (Character.isWhitespace(currentChar)) {
        currentPosition++;
        continue;
      }

      AsmToken token = nextToken();
      if (token == null) {
        return null;
      }
      tokens.add(token);
    }

    return tokens;
  }

  @Nullable
  private AsmToken nextToken() {
    if (currentPosition >= input.length()) {
      return null;
    }

    for (var rule : AsmGrammarDefaultTerminalRules.COMPILED_RULES) {
      var matcher = rule.pattern().matcher(input).region(currentPosition, input.length());

      // With lookingAt, match a substring that starts at currentPosition,
      // but does not require to match the entire region.
      if (matcher.lookingAt()) {
        String value = matcher.group();
        currentPosition += value.length();
        return new AsmToken(rule.name(), value);
      }
    }

    return null;
  }
}
