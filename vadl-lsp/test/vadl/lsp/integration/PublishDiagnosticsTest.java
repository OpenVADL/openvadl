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

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vadl.lsp.TestSnapshot;
import vadl.lsp.TestUtils;

/**
 * Tests the LSP Publish Diagnostics feature.
 *
 * <p>(Somewhat) slow test, as for every publish Diagnostic we have to wait
 * {@link vadl.lsp.VadlTextDocumentService#DIAGNOSTICS_DELAY_MS}.
 *
 * <p>Uses snapshot input files to parameterize test cases.
 */
public class PublishDiagnosticsTest extends IntegrationTest {
  private static final String INPUT_NAME = "input.vadl";

  @ParameterizedTest
  @ValueSource(strings = {"empty", "noErrors", "syntax", "noX", "twoFiles"})
  public void mainTest(String testCase) {
    var snapshot = new TestSnapshot(testCase);
    var inputUri = snapshot.getInputUri(INPUT_NAME);
    var input = snapshot.getInputData(INPUT_NAME);
    snapshot.add("Input", TestUtils.formatFileContent(input));

    var result = client.doThenWaitForPublishDiagnostics(() -> openDocument(inputUri, input));
    if (result.size() != 1) {
      fail("Expected 1 publishDiagnostics() call, but received "
          + result.size() + " within wait time");
    }

    var humanReadable = processResult(result, snapshot);
    snapshot.add("Published diagnostics", result);
    snapshot.add("... which looks like this", humanReadable);
    snapshot.verify();
  }

  private String processResult(List<PublishDiagnosticsParams> diagnosticParams,
                               TestSnapshot snapshot) {
    List<String> humanReadable = new ArrayList<>();

    for (var diagnosticParam : diagnosticParams) {
      var fileContent = snapshot.getInputData(snapshot.getInputNameFromUri(
          diagnosticParam.getUri()));
      for (var diagnostic : diagnosticParam.getDiagnostics()) {
        humanReadable.add(diagnostic.getSeverity() + ": " + diagnostic.getMessage()
            + "\n" + TestUtils.showRangeInFile(diagnostic.getRange(), fileContent));
      }
      diagnosticParam.setUri(TestUtils.normalizeUri(diagnosticParam.getUri()));
    }

    if (humanReadable.isEmpty()) {
      return "n/a";
    }
    return String.join("\n- - -\n", humanReadable);
  }
}
