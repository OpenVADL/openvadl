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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 1, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ParserUtilsTest {

  /**
   * Tests whether all tokens that are accepted as "identifier" by the generated parser
   * are also marked as "ID_TOKENS" in the lookup table.
   *
   * <p>If this test fails after adding a keyword to the {@code allowedIdentifierKeywords},
   * you probably forgot to add this keyword to the {@link ParserUtils#ID_TOKENS}.</p>
   */
  @Test
  void identifierTokens() {
    for (int i = 0; i < Parser.maxT + 1; i++) {
      var parser = parser("");
      var token = new Token();
      token.kind = i;
      token.val = "dummy";
      parser.la = token;
      var isIdToken = ParserUtils.isIdentifierToken(token);
      var parsedWithoutError = tryParse(parser::identifier);
      var wasParsedAsId = parsedWithoutError && !parser.hasErrors();

      var message = "Grammar / isIdentifierToken mismatch (token %d)".formatted(i);
      assertThat(message, isIdToken, is(wasParsedAsId));
    }
  }

  /**
   * Tests whether all tokens that are accepted as "binaryOperator" by the generated parser
   * are also marked as "BIN_OPS" in the lookup table.
   */
  @Test
  void binaryOperators() {
    for (int i = 0; i < Parser.maxT + 1; i++) {
      var parser = parser("");
      var token = new Token();
      token.kind = i;
      token.val = "dummy";
      parser.la = token;
      var isBinOpToken = ParserUtils.BIN_OPS[token.kind];
      var parsedWithoutError = tryParse(parser::binaryOperator);
      var wasParsedAsBinOp = parsedWithoutError && !parser.hasErrors();

      var message = "Grammar / BIN_OPS mismatch (token %d)".formatted(i);
      assertThat(message, isBinOpToken, is(wasParsedAsBinOp));
    }
  }

  /**
   * Tests whether all tokens that are accepted as "unaryOperator" by the generated parser
   * are also marked as "UN_OPS" in the lookup table.
   */
  @Test
  void unaryOperators() {
    for (int i = 0; i < Parser.maxT + 1; i++) {
      var parser = parser("");
      var token = new Token();
      token.kind = i;
      token.val = "dummy";
      parser.la = token;
      var isUnOpToken = ParserUtils.isUnaryOperator(token);
      var parsedWithoutError = tryParse(parser::unaryOperator);
      var wasParsedAsUnOp = parsedWithoutError && !parser.hasErrors();


      var message = "Grammar / UN_OPS mismatch (token %d)".formatted(i);
      assertThat(message, isUnOpToken, is(wasParsedAsUnOp));
    }
  }

  private Parser parser(String restProgram) {
    Parser parser = new Parser(new Scanner(new ByteArrayInputStream(restProgram.getBytes())));
    parser.t = new Token();
    parser.t.val = "dummy";
    return parser;
  }

  private boolean tryParse(Runnable parseFunc) {
    try {
      parseFunc.run();
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}