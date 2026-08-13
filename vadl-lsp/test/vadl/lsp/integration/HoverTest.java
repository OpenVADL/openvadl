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

import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vadl.lsp.TestSnapshot;
import vadl.lsp.TestUtils;

/**
 * Tests the LSP Hover feature.
 *
 * <p>Uses snapshot input files to parameterize test cases.
 */
public class HoverTest extends IntegrationTest {
  private static final String INPUT_NAME = "input.vadl";
  private static final Pattern POSITION_PATTERN =
      Pattern.compile("^// HOVER POSITION \\s*(\\d+):(\\d+)", Pattern.MULTILINE);

  @ParameterizedTest
  @ValueSource(strings = {"noHover", "constant", "calculated", "alias", "modelInvocation"})
  public void mainTest(String testCase) throws ExecutionException, InterruptedException {
    var snapshot = new TestSnapshot(testCase, true);
    var inputUri = snapshot.getInputUri(INPUT_NAME);
    var input = snapshot.getInputData(INPUT_NAME);

    var matcher = POSITION_PATTERN.matcher(input);
    if (!matcher.find()) {
      fail("Input file does not contain position to use "
          + "(Format: \"// HOVER POSITION line:character\" at the top of file): " + inputUri);
    }
    // Input files state 1-based position...
    int line = Integer.parseInt(requireNonNull(matcher.group(1)));
    int character = Integer.parseInt(requireNonNull(matcher.group(2)));
    // ... but lsp4j position is 0-based
    var position = new Position(line - 1, character - 1);
    snapshot.add("Input with position marked", TestUtils.showPositionInFile(position, input));

    // Doing this to avoid race condition in the TypeChecker with publishing diagnostics:
    // Type.bitsTypes may be modified by both at the same time (ConcurrentModificationException).
    // Unlikely for this to occur in normal operation, but this test would be flaky otherwise.
    client.doThenWaitForPublishDiagnostics(() -> openDocument(inputUri, input));
    var result = textService.hover(new HoverParams(
        new TextDocumentIdentifier(inputUri), position
    )).get();

    var rangeInFile = processResult(result, input);
    snapshot.add("returned Hover", result);
    snapshot.add("... which has this range", rangeInFile);
    snapshot.verify();
  }

  private String processResult(@Nullable Hover hover, String input) {
    if (hover == null) {
      return "n/a";
    }
    return TestUtils.showRangeInFile(hover.getRange(), input);
  }
}
