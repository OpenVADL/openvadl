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

import static vadl.ast.AstTestUtils.getResourcePath;
import static vadl.ast.AstTestUtils.loadVadlFiles;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.utils.Pair;

public class AstDumpTests {

  /**
   * Compares the computed dump of files in "test/resources/dumps/*.vadl" with the corresponding
   * expected dump in "test/resources/dumps/*.dump".
   * If a change in the code changes the dump representation, the expected dump file needs to be
   * adapted as well.
   * In this case, a file "*.dump.actual" will be generated.
   * If that file is the correct dump, simply replace the old dump with it.
   */
  @TestFactory
  Stream<DynamicTest> astDumpTests() throws URISyntaxException, IOException {
    var unitTestStream = loadVadlFiles("dumps").stream()
        .filter(path -> path.getFileName().toString().endsWith(".vadl"))
        .map(path -> DynamicTest.dynamicTest("Parse " + path.getFileName(),
            () -> assertDumpEquality(path, dumpPath(path))));

    var realWorldTestStream = Stream.of(
            Pair.of(
                getResourcePath("testSource/sys/risc-v/rv64im.vadl"),
                getResourcePath("dumps").resolve("rv64im.dump"))
        )
        .map(pair -> DynamicTest.dynamicTest("Parse " + pair.left().getFileName(),
            () -> assertDumpEquality(pair.left(), pair.right())));

    return Stream.concat(unitTestStream, realWorldTestStream);
  }

  private void assertDumpEquality(Path vadlPath, Path expectedDumpPath) throws IOException {
    var ast = VadlParser.parse(vadlPath.toAbsolutePath(), Map.of());
    ModelRemover remover = new ModelRemover();
    remover.removeModels(ast);
    Ungrouper ungrouper = new Ungrouper();
    ungrouper.ungroup(ast);
    //verifyPrettifiedAst(ast);

    var actualDumpPath = actualDumpPath(expectedDumpPath);
    var actualDump = new AstDumper().dump(ast);
    if (!Files.isRegularFile(expectedDumpPath)) {
      writeDump(actualDumpPath, actualDump);
      Assertions.fail("File " + expectedDumpPath + " does not exist");
    }
    var expectedDump = Files.readString(expectedDumpPath).replaceAll("\r\n", "\n");

    if (actualDump.equals(expectedDump)) {
      // Clean up any ".actual" files we might have left behind the last time this test failed
      Files.deleteIfExists(actualDumpPath);
    } else {
      writeDump(actualDumpPath, actualDump);
    }

    Assertions.assertEquals(expectedDump, actualDump,
        "Expected dump:\n" + expectedDump + "\nActual dump:\n" + actualDump);
  }

  private void writeDump(Path dumpPath, String dump) throws IOException {
    Files.writeString(dumpPath, dump);
  }

  private Path dumpPath(Path vadlPath) {
    var path = vadlPath.getFileName().toString().replace(".vadl", ".dump");
    return vadlPath.resolveSibling(path);
  }

  private Path actualDumpPath(Path vadlPath) {
    var path = vadlPath.getFileName().toString().replace(".dump", ".dump.actual");
    return vadlPath.resolveSibling(path);
  }
}
