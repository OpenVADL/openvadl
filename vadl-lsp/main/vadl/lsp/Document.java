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

package vadl.lsp;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.openvadl.klsp.protocol.Position;
import org.openvadl.klsp.protocol.Range;
import org.openvadl.klsp.protocol.TextDocumentContentChangeEvent;
import org.openvadl.klsp.protocol.TextDocumentItem;
import vadl.utils.SourceLocation;

/**
 * Represents one version of a file currently owned by (i.e. opened in) the LSP Client. This
 * effectively snapshots the file at one point in time.
 *
 * @see org.openvadl.klsp.protocol.TextDocumentItem
 */
public class Document {
  /**
   * End-of-line sequences defined by LSP.
   */
  private static final Pattern EOL_REGEX = Pattern.compile("(\\r\\n|\\n|\\r)");

  public final String uri;
  public final int version;

  public final List<String> textLines;

  /**
   * Creates a new Document.
   *
   * @param textLines List of individual lines. Each item MUST NOT contain a newline character.
   */
  public Document(String uri, int version, List<String> textLines) {
    this.uri = uri;
    this.version = version;
    this.textLines = Collections.unmodifiableList(textLines);
  }


  public Document(String uri, int version, String text) {
    this(uri, version, splitLines(text));
  }

  /**
   * Creates a new lsp document based on the data provided by the client.
   *
   * @param tdi as provided by the LSP didOpen request
   */
  public Document(TextDocumentItem tdi) {
    this(tdi.getUri(), tdi.getVersion(), tdi.getText());
  }

  /**
   * Creates an updated version of this document.
   */
  public Document withChanges(int newVersion, List<TextDocumentContentChangeEvent> contentChanges) {
    if (newVersion <= this.version) {
      throw new IllegalStateException(
          "Cannot update LSP document to version " + newVersion
              + " as current version is already " + this.version
      );
    }

    // Shortcuts
    if (contentChanges.isEmpty()) {
      return new Document(this.uri, newVersion, this.textLines);
    }
    if (contentChanges.size() == 1) {
      var change = contentChanges.getFirst();
      if (change.getRange() == null) {
        // Single change which replaces entire document
        return new Document(this.uri, newVersion, change.getText());
      }
    }

    List<String> newTextLines = new ArrayList<>(this.textLines);
    for (var change : contentChanges) {
      var range = change.getRange();

      if (range == null) {
        // Change replaces entire document
        newTextLines = new ArrayList<>(splitLines(change.getText()));
        continue;
      }

      // Character offsets count UTF-16 words (see server capabilities)
      int startLine = normalizeLineOffset(range.getStart().getLine(), newTextLines);
      String startLineText = newTextLines.get(startLine);
      int startCharacter = normalizeCharacterOffset(range.getStart().getCharacter(), startLineText);

      int endLine = normalizeLineOffset(range.getEnd().getLine(), newTextLines);
      String endLineText = newTextLines.get(endLine);
      int endCharacter = normalizeCharacterOffset(range.getEnd().getCharacter(), endLineText);

      if (endLine < startLine || (endLine == startLine && endCharacter < startCharacter)) {
        // Ignore invalid combination
        continue;
      }

      var insertTextLines = splitLines(
          startLineText.substring(0, startCharacter) + change.getText()
              + endLineText.substring(endCharacter)
      );

      if (endLine == startLine && insertTextLines.size() == 1) {
        // Shortcut
        newTextLines.set(startLine, insertTextLines.getFirst());
        continue;
      }

      newTextLines.subList(startLine, endLine + 1).clear();
      newTextLines.addAll(startLine, insertTextLines);
    }

    return new Document(uri, newVersion, newTextLines);
  }

  public String getText() {
    return String.join("\n", textLines);
  }

  public Path getPath() {
    return LspUtils.toPath(uri);
  }

  /**
   * Calculates LSP UTF-16 range from given VADL compiler UTF-8 location (within this document).
   *
   * @param utf8Location VADL location, which is UTF-8 1-based (end inclusive)
   * @return UTF-16 0-based (end exclusive) range
   */
  public Range calculateUtf16Range(SourceLocation utf8Location) {
    return new Range(
        calculateUtf16Position(utf8Location.begin(), false),
        calculateUtf16Position(utf8Location.end(), true)
      );
  }

  /**
   * Calculates LSP UTF-16 position from given VADL compiler UTF-8 position (within this document).
   *
   * @see #calculateUtf8Position(Position, boolean)
   * @param utf8Position VADL position, which is UTF-8 1-based
   * @param endPosition true: this is an end position, i.e. it is inclusive in VADL but exclusive in
   *                    LSP
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
      // No update of previousTargetPos - next token is calculated based on start pos of the
      // current token
    }

    return semanticTokens;
  }

  /**
   * Calculates VADL UTF-8 position from given LSP UTF-16 position (within this document).
   *
   * @see #calculateUtf16Position(SourceLocation.Position, boolean)
   * @param utf16Position LSP position, which is UTF-16 0-based
   * @param endPosition true: this is an end position, i.e. it is exclusive in LSP but inclusive in
   *                    VADL
   * @return UTF-8 1-based position
   */
  public SourceLocation.Position calculateUtf8Position(
      Position utf16Position, boolean endPosition) {
    int line = utf16Position.getLine();
    int character = utf16Position.getCharacter();
    int column = character;

    String lineText = textLines.get(line);
    for (int i = 0; i < character; i++) {
      column += utf8Utf16LengthDifference(lineText.charAt(i));
    }

    // Change from 0-based to 1-based ...
    line += 1;
    // ... but end positions are inclusive in VADL
    column += (endPosition ? 0 : 1);

    return new SourceLocation.Position(line, column);
  }


  /**
   * Splits the given String into individual lines (as stored in {@code textLines}).
   *
   * @return text lines. This List has a fixed size! (see {@link Arrays#asList(Object[])})
   */
  private static List<String> splitLines(String text) {
    // Note: We don't distinguish between the different line endings; and as long as this server
    //       doesn't make editing suggestions to the client (which may use different eol sequences
    //       than the user) this should be fine.
    return Arrays.asList(EOL_REGEX.split(text, -1));
  }

  private static int normalizeLineOffset(int lineOffset, List<String> textLines) {
    return Math.clamp(lineOffset, 0, textLines.size() - 1);
  }

  private static int normalizeCharacterOffset(int characterOffset, String textLine) {
    // According to LSP spec, character offsets that are too large shall be interpreted as the
    // maximum value for the resp. text line.
    return Math.clamp(characterOffset, 0, textLine.length());
  }

  private static int utf8Utf16LengthDifference(char character) {
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
