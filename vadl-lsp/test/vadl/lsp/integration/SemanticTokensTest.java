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
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vadl.lsp.TestSnapshot;
import vadl.lsp.TestUtils;

/**
 * Tests the LSP Semantic Tokens feature.
 *
 * <p>Uses snapshot input files to parameterize test cases.
 */
public class SemanticTokensTest extends IntegrationTest {
  private static final String INPUT_NAME = "input.vadl";

  @ParameterizedTest
  @ValueSource(strings = {"empty", "constantConstant", "modelDefinition", "twoFiles"})
  public void mainTest(String testCase) throws ExecutionException, InterruptedException {
    var snapshot = new TestSnapshot(testCase, true);
    var inputUri = snapshot.getInputUri(INPUT_NAME);
    var input = snapshot.getInputData(INPUT_NAME);
    snapshot.add("Input", TestUtils.formatFileContent(input));

    openDocument(inputUri, input);
    var result = textService.semanticTokensFull(new SemanticTokensParams(
        new TextDocumentIdentifier(inputUri)
    )).get();

    snapshot.add("returned tokens (LSP numeric format)",
        processResultTable(result.getData()));
    snapshot.add("... which look like this", processResultInFile(result.getData(), input));
    snapshot.addNote("We're using one token type (variable) and no modifiers,"
        + " so all ranges shown above should be <0>");
    snapshot.verify();
  }

  private String processResultTable(List<Integer> tokens) {
    if (tokens.isEmpty()) {
      return "[]";
    }

    var text = new StringBuilder("deltaLine, deltaStart, length, tokenType, tokenModifiers\n");
    for (int i = 0; i < tokens.size(); i++) {
      text.append(tokens.get(i));
      text.append((i % 5 == 4) ? "\n" : ", ");
    }

    return text.toString().trim();
  }

  private String processResultInFile(List<Integer> tokens, String input) {
    if (tokens.isEmpty()) {
      return "n/a";
    }

    List<TestUtils.NamedRange> ranges = new ArrayList<>(tokens.size() / 5);
    int line = 0;
    int column = 0;
    for (int i = 0; i < tokens.size(); i += 5) {

      line += tokens.get(i); // deltaLine
      if (tokens.get(i) != 0) {
        column = 0;
      }
      column += tokens.get(i + 1); // deltaStart

      int length = tokens.get(i + 2);
      int tokenType = tokens.get(i + 3);
      int tokenModifiers = tokens.get(i + 4);

      String name = "" + tokenType + (tokenModifiers != 0 ? "-" + tokenModifiers : "");
      ranges.add(new TestUtils.NamedRange(
          new Range(new Position(line, column), new Position(line, column + length)),
          name
      ));
    }

    return TestUtils.showRangesInFile(ranges, input);
  }
}
