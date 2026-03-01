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

package vadl.lsp;

import java.util.List;
import org.eclipse.lsp4j.Position;
import vadl.utils.SourceLocation;

/**
 * A snapshot of a {@link Document}. I.e. while the original document object is mutable, this
 * snapshot does not change.
 *
 * @see Document
 * @see Document#getSnapshot()
 */
public record DocumentSnapshot(String uri, int version, String text, List<String> textLines) {

  /**
   * Calculates LSP UTF-16 position from given VADL compiler UTF-8 position (within this document).
   *
   * @param utf8Position VADL position, which is UTF-8 1-based
   * @param endPosition This is an end position, i.e. it is inclusive in VADL but exclusive in LSP
   * @return UTF-16 0-based position
   */
  public Position calculateUtf16Position(
      SourceLocation.Position utf8Position, boolean endPosition) {
    // Change from 1-based to 0-based ...
    int line = Math.max(utf8Position.line() - 1, 0);
    // ... but end positions are exclusive in LSP
    int column = Math.max(utf8Position.column() - (endPosition ? 0 : 1), 0);

    String lineText = textLines.get(line);
    for (int i = 0; i < column; i++) {
      column -= utf8Utf16LengthDifference(lineText.charAt(i));
    }

    return new Position(line, column);
  }

  /**
   * Calculates LSP UTF-16 position from given VADL compiler UTF-8 position, which are given in
   * the special optimized Semantic Tokens format.
   *
   * @param semanticTokens Token list encoded for a semanticTokens response, with UTF-8 positions,
   *                       tokens do not span multiple lines.
   * @return {@code semanticTokens} but with correct UTF-16 positions
   */
  public List<Integer> calculateUtf16Positions(
      List<Integer> semanticTokens) {
    if (semanticTokens.isEmpty()) {
      return semanticTokens;
    }

    int line = 0;
    String lineText = textLines.getFirst();
    int linePos;
    int previousTargetPos = 0;
    int targetPos = semanticTokens.get(1);
    for (int i = 0; i < semanticTokens.size(); i += 5) {
      // semanticTokens: deltaLine, deltaStart, length, tokenType, tokenModifiers

      int deltaLine = semanticTokens.get(i);
      if (deltaLine != 0) {
        line += deltaLine;
        lineText = textLines.get(line);
        previousTargetPos = 0;
      }
      linePos = previousTargetPos;
      targetPos = semanticTokens.get(i + 1) + previousTargetPos;

      // deltaStart
      for (; linePos < targetPos; linePos++) {
        targetPos -= utf8Utf16LengthDifference(lineText.charAt(linePos));
      }
      semanticTokens.set(i + 1, targetPos - previousTargetPos);
      previousTargetPos = targetPos;

      // length
      targetPos = targetPos + semanticTokens.get(i + 2);
      for (; linePos < targetPos; linePos++) {
        targetPos -= utf8Utf16LengthDifference(lineText.charAt(linePos));
      }
      semanticTokens.set(i + 2, targetPos - previousTargetPos);
      // No update of previousTargetPos - next token is calculated based on start pos of the current
      // token
    }

    return semanticTokens;
  }

  /**
   * Returns the UTF-16 length of a text line.
   *
   * @param line 0-based line index
   * @return length of selected text line (in amount of UTF-16 words)
   */
  public int getUtf16LineLength(int line) {
    return textLines.get(line).length();
  }

  /**
   * Returns the UTF-8 length of a text line.
   *
   * @param line 0-based line index
   * @return length of selected text line (in amount of UTF-8 bytes)
   */
  public int getUtf8LineLength(int line) {
    String lineText = textLines.get(line);
    int length = 0;
    for (int i = 0; i < lineText.length(); i++) {
      length += 1 + utf8Utf16LengthDifference(lineText.charAt(i));
    }
    return length;
  }


  private int utf8Utf16LengthDifference(char character) {
    // UTF-8 position counts bytes; UTF-16 position counts 16bit words
    // E.g. if a character needs 2 bytes in UTF-8 and 16bit in UTF-16, the difference is 1

    if (character < 128) {
      // UTF-8: 1 byte
      return 0;
    }
    if (character < 0x800) {
      // UTF-8: 2 bytes
      return 1;
    }
    if (character >= 0xD800 && character <= 0xDFFF) {
      // UTF-16: 2 words (surrogate pair - above Basic Multilingual Plane)
      // UTF-8: 4 bytes
      // => 1 difference per UTF-16 word
      return 1;
    }
    // UTF-8: 3 bytes
    return 2;
  }
}
