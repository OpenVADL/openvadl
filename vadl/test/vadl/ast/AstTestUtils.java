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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;

public class AstTestUtils {

  private static final ModelRemover MODEL_REMOVER = new ModelRemover();
  private static final Ungrouper UNGROUPER = new Ungrouper();

  static void verifyPrettifiedAst(Ast ast) {
    MODEL_REMOVER.removeModels(ast);
    UNGROUPER.ungroup(ast);
    var progPretty = ast.prettyPrintToString();
    var astPretty = Assertions.assertDoesNotThrow(() -> VadlParser.parse(progPretty, ast.fileUri),
        "Cannot parse prettified input \n" + progPretty);
    UNGROUPER.ungroup(astPretty);
    assertAstEquality(astPretty, ast);
  }

  static void assertAstEquality(Ast actual, Ast expected) {
    MODEL_REMOVER.removeModels(actual);
    MODEL_REMOVER.removeModels(expected);
    UNGROUPER.ungroup(actual);
    UNGROUPER.ungroup(expected);
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
