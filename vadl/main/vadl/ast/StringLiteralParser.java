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
