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

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vadl.lsp.TestSnapshot;
import vadl.lsp.TestUtils;

/**
 * Tests the LSP (Goto) Definition feature.
 *
 * <p>Uses snapshot input files to parameterize test cases.
 */
public class GotoDefinitionTest extends IntegrationTest {
  private static final String INPUT_NAME = "input.vadl";
  private static final Pattern POSITION_PATTERN =
      Pattern.compile("^// GOTO POSITION \\s*(\\d+):(\\d+)", Pattern.MULTILINE);

  @ParameterizedTest
  @ValueSource(strings = {"noDefinition", "aliasRegister", "isa", "twoFiles"})
  public void mainTest(String testCase) throws ExecutionException, InterruptedException {
    var snapshot = new TestSnapshot(testCase, true);
    var inputUri = snapshot.getInputUri(INPUT_NAME);
    var input = snapshot.getInputData(INPUT_NAME);

    var matcher = POSITION_PATTERN.matcher(input);
    if (!matcher.find()) {
      fail("Input file does not contain position to use "
          + "(Format: \"// GOTO POSITION line:character\" at the top of file): " + inputUri);
    }
    // Input files state 1-based position...
    int line = Integer.parseInt(requireNonNull(matcher.group(1)));
    int character = Integer.parseInt(requireNonNull(matcher.group(2)));
    // ... but lsp4j position is 0-based
    var position = new Position(line - 1, character - 1);
    snapshot.add("Input with position marked", TestUtils.showPositionInFile(position, input));

    openDocument(inputUri, input);
    var result = textService.definition(new DefinitionParams(
        new TextDocumentIdentifier(inputUri), position
    )).get();

    var rangesInFiles = processResult(result, input, snapshot);
    snapshot.add("returned Goto Definition", result);
    snapshot.add("... which looks like this", rangesInFiles);
    snapshot.verify();
  }

  private String processResult(
      Either<List<? extends Location>, List<? extends LocationLink>> definitionResult,
      String input, TestSnapshot snapshot) {

    List<String> rangesInFiles = new ArrayList<>();

    if (definitionResult.isLeft()) {
      for (var location : definitionResult.getLeft()) {
        rangesInFiles.add(TestUtils.showRangeInFile(location.getRange(),
            snapshot.getInputData(snapshot.getInputNameFromUri(location.getUri()))));
        location.setUri(TestUtils.normalizeUri(location.getUri()));
      }

    } else {
      for (var link : definitionResult.getRight()) {
        var targetFileName = snapshot.getInputNameFromUri(link.getTargetUri());
        var targetFileContent = snapshot.getInputData(targetFileName);
        var targetFileRanges = new ArrayList<>(List.of(
            new TestUtils.NamedRange(link.getTargetSelectionRange(), "TARGET SELECTION RANGE"),
            new TestUtils.NamedRange(link.getTargetRange(), "TARGET RANGE")
        ));
        var originSelectionRange = new TestUtils.NamedRange(link.getOriginSelectionRange(),
            "ORIGIN SELECTION RANGE");

        if (targetFileName.equals(INPUT_NAME)) {
          targetFileRanges.add(originSelectionRange);
          rangesInFiles.add(TestUtils.showRangesInFile(targetFileRanges, targetFileContent));
        } else {
          rangesInFiles.add(TestUtils.showRangesInFile(targetFileRanges, targetFileContent));
          rangesInFiles.add(TestUtils.showRangesInFile(List.of(originSelectionRange), input));
        }

        link.setTargetUri(TestUtils.normalizeUri(link.getTargetUri()));
      }
    }

    if (rangesInFiles.isEmpty()) {
      return "n/a";
    }
    return String.join("\n", rangesInFiles);
  }
}
