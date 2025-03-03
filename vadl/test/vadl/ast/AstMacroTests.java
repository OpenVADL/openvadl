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

import static vadl.ast.AstTestUtils.loadVadlFiles;
import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class AstMacroTests {

  private static final String MACRO_REPLACEMENTS_MARKER = "/// MACRO REPLACEMENTS";

  /**
   * Compares the computed expanded AST of files in "test/resources/macros/*.vadl" with the
   * corresponding expected dump in "test/resources/macros/*.expanded.vadl".
   * If a change in the code changes the AST representation, the expanded AST file needs to be
   * adapted as well.
   * In this case, a file "*.actual.expanded.vadl" will be generated.
   * If that file is a correct AST, simply replace the old AST with it.
   * To pass macro overrides like the CLI "-m" flag, create a comment section
   * anywhere in the VADL file with the following template:
   * {@code
   * /// MACRO REPLACEMENTS
   * /// key=value
   * /// key=value
   * /// key=value
   * /// MACRO REPLACEMENTS
   * }
   * Any key-value pairs between the MACRO REPLACEMENTS section will be interpreted as macro
   * overrides.
   */
  @TestFactory
  Stream<DynamicTest> astMacroTests() throws URISyntaxException, IOException {
    return loadVadlFiles("macros").stream()
        .filter(path -> path.getFileName().toString().endsWith(".vadl")
            && !path.getFileName().toString().endsWith(".expanded.vadl")
            && Files.isRegularFile(path))
        .map(path -> DynamicTest.dynamicTest("Parse " + path.getFileName(),
            () -> assertAstEquality(path)));
  }

  private void assertAstEquality(Path vadlPath) throws IOException {
    var replacements = parseMacroReplacements(vadlPath.toAbsolutePath());
    var ast = VadlParser.parse(vadlPath.toAbsolutePath(), replacements);
    verifyPrettifiedAst(ast);

    var actualExpandedAst = ast.prettyPrintToString();
    var expectedAstPath = expandedAstPath(vadlPath);
    if (!Files.isRegularFile(expectedAstPath)) {
      writeAst(actualAstPath(vadlPath), actualExpandedAst);
      Assertions.fail("File " + expectedAstPath + " does not exist");
    }
    var expectedAst = Files.readString(expectedAstPath).replaceAll("\r\n", "\n");

    Files.deleteIfExists(actualAstPath(vadlPath));
    writeAst(actualAstPath(vadlPath), actualExpandedAst);
    Assertions.assertLinesMatch(expectedAst.lines(), actualExpandedAst.lines());
  }

  private Map<String, String> parseMacroReplacements(Path vadlPath) throws IOException {
    var content = Files.readString(vadlPath);
    return content.lines()
        .dropWhile(line -> !line.equals(MACRO_REPLACEMENTS_MARKER))
        .skip(1)
        .takeWhile(line -> !line.equals(MACRO_REPLACEMENTS_MARKER))
        .map(line -> line.substring(line.indexOf("/// ") + 4))
        .collect(Collectors.toMap(
            line -> line.substring(0, line.indexOf("=")),
            line -> line.substring(line.indexOf("=") + 1)));
  }

  private void writeAst(Path astPath, CharSequence ast) throws IOException {
    Files.writeString(astPath, ast);
  }

  private Path expandedAstPath(Path vadlPath) {
    var path = vadlPath.getFileName().toString().replace(".vadl", ".expanded.vadl");
    return vadlPath.resolveSibling(path);
  }

  private Path actualAstPath(Path vadlPath) {
    var path = vadlPath.getFileName().toString().replace(".vadl", ".actual.expanded.vadl");
    return vadlPath.resolveSibling(path);
  }
}
