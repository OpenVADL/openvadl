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

package vadl.lsp.integration;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;
import vadl.lsp.TestSnapshot;
import vadl.lsp.TestUtils;

/**
 * Tests some properties regarding correct document syncing between client and server.
 */
public class DocumentSyncTest extends IntegrationTest {
  private static final String URI         = "file:///c:/doesnotexist/file.vadl";
  private static final String URI_ENCODED = "file:///c%3A/doesnotexist/file.vadl";
  private static final String INPUT = "constant foo = 42\nconstant bar = foo";
  // Don't forget these LSP Positions are 0-based
  private static final Position DEFINITION_POSITION = new Position(1, 16);
  private static final Position INSERT_POSITION = new Position(1, 15);

  /**
   * Tests peculiarities of paths on Windows: The colon after the drive letter may or may not be
   * url-encoded. See {@link #URI} vs. {@link #URI_ENCODED}. Both URIs MUST be interpreted as the
   * same file.
   *
   * @see <a href="https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#uri">Note in LSP specs</a>
   */
  @Test
  public void windowsUri() throws ExecutionException, InterruptedException {
    var snapshot = new TestSnapshot();
    String uri = URI;

    openDocument(uri, INPUT);
    snapshot.add("Opened file with URI", uri);

    snapshot.add("Input with desired position marked",
        TestUtils.showPositionInFile(DEFINITION_POSITION, INPUT));

    uri = URI_ENCODED;
    var definitionParams = new DefinitionParams(
        new TextDocumentIdentifier(uri), DEFINITION_POSITION);
    snapshot.addNote(
        "different URI, but should be considered equal to the URI of the file we opened above");
    snapshot.add("Requesting Goto Definition", definitionParams);

    var definitionResult = textService.definition(definitionParams).get();
    snapshot.addNote("should point to \"foo\" on first line");
    snapshot.add("Returned Goto Definition", definitionResult);
    snapshot.add("... which looks like this", processDefinitionRanges(definitionResult));

    uri = URI_ENCODED;
    var changes = new ArrayList<TextDocumentContentChangeEvent>(1);
    changes.add(new TextDocumentContentChangeEvent(
        new Range(INSERT_POSITION, INSERT_POSITION), "    "
    ));
    var changeParams = new DidChangeTextDocumentParams(
        new VersionedTextDocumentIdentifier(uri, 1), changes);
    snapshot.addNote("still the second URI");
    snapshot.add("Changing document (inserting whitespace before \"foo\")", changeParams);
    textService.didChange(changeParams);

    uri = URI;
    definitionParams = new DefinitionParams(
        new TextDocumentIdentifier(uri), DEFINITION_POSITION);
    snapshot.addNote("first URI this time");
    snapshot.add("Requesting Goto Definition again", definitionParams);

    definitionResult = textService.definition(definitionParams).get();
    snapshot.addNote(
        "should point nowhere, as requested position is in the middle of whitespace now");
    snapshot.add("Returned Goto Definition", definitionResult);

    snapshot.verify();
  }

  private String processDefinitionRanges(
      Either<List<? extends Location>, List<? extends LocationLink>> definitionResult) {
    var locationLink = definitionResult.getRight().getFirst();

    return TestUtils.showRangesInFile(List.of(
        new TestUtils.NamedRange(locationLink.getTargetSelectionRange(), "TARGET SELECTION RANGE"),
        new TestUtils.NamedRange(locationLink.getTargetRange(), "TARGET RANGE"),
        new TestUtils.NamedRange(locationLink.getOriginSelectionRange(), "ORIGIN SELECTION RANGE")
    ), INPUT);
  }
}
