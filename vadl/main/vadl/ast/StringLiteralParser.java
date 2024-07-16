package vadl.ast;

public class StringLiteralParser {

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
