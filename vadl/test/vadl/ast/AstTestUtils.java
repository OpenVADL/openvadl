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

package vadl.ast;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import vadl.utils.OverlayVirtualFileSystem;
import vadl.utils.SingleFileVirtualFileSystem;
import vadl.utils.VirtualFileSystem;

public class AstTestUtils {


  static void verifyPrettifiedAst(Ast ast) {
    ModelRemover.removeModels(ast);
    Ungrouper.ungroup(ast);
    var progPretty = ast.prettyPrintToString();
    var astPretty = Assertions.assertDoesNotThrow(() -> VadlParser.parse(progPretty, ast.filePath),
        "Cannot parse prettified input \n" + progPretty);
    Ungrouper.ungroup(astPretty);
    assertAstEquality(astPretty, ast);
  }

  static void verifyPrettifiedAst(Ast ast, VirtualFileSystem fileSystem) {
    ModelRemover.removeModels(ast);
    Ungrouper.ungroup(ast);
    var progPretty = ast.prettyPrintToString();
    var injectedFileSystem = new OverlayVirtualFileSystem(new SingleFileVirtualFileSystem(progPretty,
        ast.filePath), fileSystem);
    var astPretty =
        Assertions.assertDoesNotThrow(() -> VadlParser.parse(ast.filePath, injectedFileSystem),
        "Cannot parse prettified input \n" + progPretty);
    Ungrouper.ungroup(astPretty);
    assertAstEquality(astPretty, ast);
  }

  static void assertAstEquality(Ast actual, Ast expected) {
    ModelRemover.removeModels(actual);
    ModelRemover.removeModels(expected);
    Ungrouper.ungroup(actual);
    Ungrouper.ungroup(expected);
    if (!actual.equals(expected)) {
      Assertions.assertEquals(actual, expected, AstDiffPrinter.printDiff(actual, expected));
    }
  }

  static Path getResourcePath(String directory) throws URISyntaxException {
    var dir = Objects.requireNonNull(AstTestUtils.class.getClassLoader().getResource(directory));
    var sourceDir = Path.of(dir.toURI()).toAbsolutePath().toString()
        .replace("/build/resources/test", "/test/resources");
    return Path.of(sourceDir);
  }

  static List<Path> loadVadlFiles(String directory) throws URISyntaxException, IOException {
    try (Stream<Path> files = Files.list(getResourcePath(directory))) {
      return files.toList();
    }
  }
}
