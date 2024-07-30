package vadl.ast;

/**
 * Parses any valid VADL string literal into the represented {@link java.lang.String} value.
 * An escape sequence starts with a backslash, followed by one or more characters.
 * Supported escape sequences are:
 * <ul>
 *   <li>\ - Resolves to a single backslash</li>
 *   <li>" - Resolves to a double quote</li>
 *   <li>' - Resolves to a single quote</li>
 *   <li>b - Resolves to a backspace character</li>
 *   <li>t - Resolves to a tab character</li>
 *   <li>r - Resolves to a carriage return character</li>
 *   <li>n - Resolves to a newline character</li>
 *   <li>f - Resolves to a form feed character</li>
 *   <li>uXXXX where XXXX is a four-digit hex unicode codepoint</li>
 * </ul>
 *
 */
class StringLiteralParser {

  static String parseString(String token) {
    final StringBuilder str = new StringBuilder();
    for (int i = 0; i < token.length(); i++) {
      char c = token.charAt(i);
      if (c == '\\') {
        if (i == token.length() - 1) {
          throw new IllegalArgumentException("Invalid escape sequence: \\ at end of string");
        }
        switch (token.charAt(++i)) {
          case 'b':
            str.append('\b');
            break;
          case 't':
            str.append('\t');
            break;
          case 'n':
            str.append('\n');
            break;
          case 'f':
            str.append('\f');
            break;
          case 'r':
            str.append('\r');
            break;
          case '"':
            str.append('"');
            break;
          case '\'':
            str.append('\'');
            break;
          case '\\':
            str.append('\\');
            break;
          case 'u':
            str.append(parseUnicodeSequence(token, ++i));
            i += 3;
            break;
          default:
            throw new IllegalArgumentException("Invalid escape sequence: \\" + token.charAt(i));
        }
      } else {
        str.append(c);
      }
    }
    return str.toString();
  }

  private static String parseUnicodeSequence(String token, int i) {
    if (token.length() < i + 4) {
      throw new IllegalArgumentException("Invalid unicode escape sequence found in " + token);
    }
    int codePoint = Integer.parseInt(token.substring(i, i + 4), 16);
    return Character.toString(codePoint);
  }
}
