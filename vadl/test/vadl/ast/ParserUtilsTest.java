package vadl.ast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 1, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class ParserUtilsTest {

  /**
   * Tests whether all tokens that are accepted as "identifier" by the generated parser
   * are also marked as "ID_TOKENS" in the lookup table.
   */
  @Test
  void identifierTokens() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (int i = 0; i < Parser.maxT + 1; i++) {
      var parser = parser("", out);
      var token = new Token();
      token.kind = i;
      token.val = "dummy";
      parser.la = token;
      var isIdToken = ParserUtils.isIdentifierToken(token);
      var parsedWithoutError = tryParse(parser::identifier);
      var wasParsedAsId = parsedWithoutError && out.size() == 0;

      var message = "Grammar / isIdentifierToken mismatch (token %d)".formatted(i);
      assertThat(message, isIdToken, is(wasParsedAsId));
      out.reset();
    }
  }

  /**
   * Tests whether all tokens that are accepted as "binaryOperator" by the generated parser
   * are also marked as "BIN_OPS" in the lookup table.
   */
  @Test
  void binaryOperators() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (int i = 0; i < Parser.maxT + 1; i++) {
      var parser = parser("", out);
      var token = new Token();
      token.kind = i;
      token.val = "dummy";
      parser.la = token;
      var isBinOpToken = ParserUtils.BIN_OPS[token.kind];
      var parsedWithoutError = tryParse(parser::binaryOperator);
      var wasParsedAsBinOp = parsedWithoutError && out.size() == 0;

      var message = "Grammar / BIN_OPS mismatch (token %d)".formatted(i);
      assertThat(message, isBinOpToken, is(wasParsedAsBinOp));
      out.reset();
    }
  }

  /**
   * Tests whether all tokens that are accepted as "auxiliaryField" types by the generated parser
   * are also marked as "AUX_FIELD_TOKENS" in the lookup table.
   */
  @Test
  void auxiliarFieldTokens() {
    var restProgram = "{ id => 0 }";
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (int i = 0; i < Parser.maxT + 1; i++) {
      var parser = parser(restProgram, out);
      var token = new Token();
      token.kind = i;
      token.val = "dummy";
      parser.la = token;
      var isAuxFieldToken = ParserUtils.AUX_FIELD_TOKENS[token.kind];
      var parsedWithoutError = tryParse(parser::auxiliaryField);
      var wasParsedAsAuxField = parsedWithoutError && out.size() == 0;

      var message = "Grammar / AUX_FIELD_TOKENS mismatch (token %d)".formatted(i);
      assertThat(message, isAuxFieldToken, is(wasParsedAsAuxField));
      out.reset();
    }
  }

  private Parser parser(String restProgram, ByteArrayOutputStream outputStream) {
    Parser parser = new Parser(new Scanner(new ByteArrayInputStream(restProgram.getBytes())));
    parser.t = new Token();
    parser.t.val = "dummy";
    parser.errors.errorStream = new PrintStream(outputStream);
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