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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.jupiter.api.Test;
import vadl.utils.SourceLocation;

/**
 * Tests for language Server Document.
 */
public class DocumentTest {
  static final String TEST_URI = "file:///virtual/file.vadl";
  static final String TEST_URI2 = "file:///somewhere/else/test.vadl";

  static final List<String> TEST_LINES = List.of(
      "constant jakob = 42",
      "  // Some comment",
      "",
      "constant flo = jakob"
  );
  static final String TEST_TEXT = String.join("\n", TEST_LINES);

  static final List<String> TEST_LINES2 = List.of(
      "/* This is a test file",
      "   that doesn't actually exist! */",
      "constant schubert = 5",
      "",
      "constant rupert = 12"
  );
  static final String TEST_TEXT2 = String.join("\n", TEST_LINES2);

  @SuppressWarnings("checkstyle:AvoidEscapedUnicodeCharacters")
  static final List<String> TEST_UNICODE_LINES = List.of(
      "// ä: 2 byte in UTF-8",
      "// ∈: 3 byte in UTF-8",
      "// \uD834\uDD1E: 4 byte in UTF-8 & surrogate pair in UTF-16"
  );
  static final String TEST_UNICODE_TEXT = String.join("\n", TEST_UNICODE_LINES);


  @Test
  void textLinesConstructor() {
    Document document = new Document(TEST_URI, 13, TEST_LINES);

    assertThat(document.uri).isEqualTo(TEST_URI);
    assertThat(document.version).isEqualTo(13);
    assertThat(document.textLines).isEqualTo(TEST_LINES);
    assertThat(document.getText()).isEqualTo(TEST_TEXT);
  }

  @Test
  void textConstructor() {
    Document document = new Document(TEST_URI2, 6, TEST_TEXT);

    assertThat(document.uri).isEqualTo(TEST_URI2);
    assertThat(document.version).isEqualTo(6);
    assertThat(document.textLines).isEqualTo(TEST_LINES);
    assertThat(document.getText()).isEqualTo(TEST_TEXT);
  }

  @Test
  void textDocumentItemConstructor() {
    var tdi = new TextDocumentItem(TEST_URI, "vadl", 0, TEST_TEXT2);

    Document document = new Document(tdi);

    assertThat(document.uri).isEqualTo(TEST_URI);
    assertThat(document.version).isEqualTo(0);
    assertThat(document.textLines).isEqualTo(TEST_LINES2);
    assertThat(document.getText()).isEqualTo(TEST_TEXT2);
  }

  @Test
  void withChanges_failsIfInvalidVersion() {
    Document document = new Document(TEST_URI, 5, TEST_TEXT);

    assertThrows(IllegalStateException.class, () -> document.withChanges(4, List.of()));
  }

  @Test
  void withChanges() {
    var snapshot = new TestSnapshot();
    Document document = new Document(TEST_URI, 0, TEST_TEXT);
    snapshot.add("Initial document", document);

    // 1)
    var contentChanges = List.of(
        new TextDocumentContentChangeEvent(TEST_TEXT2)
    );
    snapshot.add("1st change (full text replaced)", contentChanges);

    document = document.withChanges(1, contentChanges);

    assertThat(document.getText()).isEqualTo(TEST_TEXT2);
    snapshot.add("Changed document (1)", document);

    // 2)
    contentChanges = List.of(
        new TextDocumentContentChangeEvent(new Range(
            new Position(2, 9),
            new Position(2, 17)
        ), "haydn")
    );
    snapshot.addNote("This should replace \"schubert\" with \"haydn\"");
    snapshot.add("2nd change (single-line replacement)", contentChanges);

    document = document.withChanges(2, contentChanges);
    snapshot.add("Changed document (2)", document);

    // 3)
    contentChanges = List.of(
        new TextDocumentContentChangeEvent(new Range(
            new Position(1, 32),
            new Position(1, 32)
        ), "like, at all! ")
    );
    snapshot.addNote("This should add more text at the end of the multi-line comment");
    snapshot.add("3rd change (single-line insertion)", contentChanges);

    document = document.withChanges(3, contentChanges);
    snapshot.add("Changed document (3)", document);

    // 4)
    contentChanges = List.of(
        new TextDocumentContentChangeEvent(new Range(
            new Position(0, 13),
            new Position(0, 18)
        ), "")
    );
    snapshot.addNote("This should remove \"test\"");
    snapshot.add("4th change (single-line removal)", contentChanges);

    document = document.withChanges(4, contentChanges);
    snapshot.add("Changed document (4)", document);

    // 5)
    contentChanges = List.of(
        new TextDocumentContentChangeEvent(new Range(
            new Position(0, 2),
            new Position(1, 46)
        ), "")
    );
    snapshot.addNote("This should remove everything within the comment");
    snapshot.add("5th change (multi-line removal)", contentChanges);

    document = document.withChanges(5, contentChanges);
    snapshot.add("Changed document (5)", document);

    // 6)
    contentChanges = List.of(
        new TextDocumentContentChangeEvent(new Range(
            new Position(1, 18),
            new Position(1, 18)
        ), "\n// Something new:\nconstant mozart = 33")
    );
    snapshot.addNote("This should add two new lines after haydn");
    snapshot.add("6th change (multi-line insertion)", contentChanges);

    document = document.withChanges(6, contentChanges);
    snapshot.add("Changed document (6)", document);

    // 7)
    contentChanges = List.of(
        new TextDocumentContentChangeEvent(new Range(
            new Position(2, 0),
            new Position(4, 0)
        ), "")
    );
    snapshot.addNote("... and removed them again");
    snapshot.add("7th change (multi-line removal)", contentChanges);

    document = document.withChanges(7, contentChanges);
    snapshot.add("Changed document (7)", document);

    // Fin
    snapshot.addNote("That is all.");
    snapshot.verify();
  }

  @Test
  public void positionCalculation_asciiOnly() {
    Document document = new Document(TEST_URI2, 0, TEST_TEXT2);

    var vadlPosition = new SourceLocation.Position(2, 25);

    var lspPosition = document.calculateUtf16Position(vadlPosition, false);
    assertThat(lspPosition).extracting("line", "character")
        .containsExactly(1, 24);
    assertThat(document.calculateUtf8Position(lspPosition, false))
        .isEqualTo(vadlPosition);

    lspPosition = document.calculateUtf16Position(vadlPosition, true);
    assertThat(lspPosition).extracting("line", "character")
        .containsExactly(1, 25);
    assertThat(document.calculateUtf8Position(lspPosition, true))
        .isEqualTo(vadlPosition);
  }

  @Test
  public void rangeCalculation_asciiOnly() {
    Document document = new Document(TEST_URI, 0, TEST_TEXT);

    var vadlRange = SourceLocation.of(null,
        new SourceLocation.Position(1, 3),
        new SourceLocation.Position(2, 15),
        null
    );

    var lspRange = document.calculateUtf16Range(vadlRange);
    assertThat(lspRange.getStart()).extracting("line", "character")
        .containsExactly(0, 2);

    assertThat(lspRange.getEnd()).extracting("line", "character")
        .containsExactly(1, 15);
  }

  @Test
  public void positionCalculation_unicode() {
    Document document = new Document(TEST_URI, 0, TEST_UNICODE_TEXT);

    // 2 byte UTF-8
    var vadlPosition = new SourceLocation.Position(1, 15);
    var lspPosition = document.calculateUtf16Position(vadlPosition, false);
    assertThat(lspPosition).extracting("line", "character")
        .containsExactly(0, 13);
    assertThat(document.calculateUtf8Position(lspPosition, false))
        .isEqualTo(vadlPosition);

    // 3 byte UTF-8
    vadlPosition = new SourceLocation.Position(2, 15);
    lspPosition = document.calculateUtf16Position(vadlPosition, false);
    assertThat(lspPosition).extracting("line", "character")
        .containsExactly(1, 12);
    assertThat(document.calculateUtf8Position(lspPosition, false))
        .isEqualTo(vadlPosition);

    // 4 byte UTF-8
    vadlPosition = new SourceLocation.Position(3, 15);
    lspPosition = document.calculateUtf16Position(vadlPosition, false);
    assertThat(lspPosition).extracting("line", "character")
        .containsExactly(2, 12);
    assertThat(document.calculateUtf8Position(lspPosition, false))
        .isEqualTo(vadlPosition);
  }
}
