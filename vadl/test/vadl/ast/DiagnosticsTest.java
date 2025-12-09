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

import static vadl.TestUtils.assertEqualsFileContent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.error.DiagnosticPrinter;
import vadl.viam.passes.verification.ViamVerifier;

/**
 * Runs all files in the test/resources/diagnostics directory.
 * The files are fed into the compilation pipeline and the thrown diagnostics are stored.
 * Then the original file is read again and compared if it reported the same diagnostics.
 *
 * <p>To update the snapshots, set the environment variable UPDATE_SNAPSHOTS.
 * {@code UPDATE_SNAPSHOTS=true ./gradlew test --tests vadl.ast.DiagnosticsTest}
 */
public class DiagnosticsTest {
  @TestFactory
  Stream<DynamicTest> snapshotTests() throws IOException {
    return Files.walk(Paths.get("test/resources/diagnostics"))
        .filter(path -> path.toString().endsWith(".vadl"))
        .map(path -> DynamicTest.dynamicTest(
            path.toString(),
            () -> runSnapshotTest(path)
        ));
  }

  private Pattern commentPattern =
      Pattern.compile("// Reported Diagnostics:.*$", Pattern.DOTALL);

  void runSnapshotTest(Path path) throws IOException {

    List<Diagnostic> diagnostics = List.of();
    try {
      var ast = VadlParser.parse(path);
      var remover = new ModelRemover();
      remover.removeModels(ast);
      var ungrouper = new Ungrouper();
      ungrouper.ungroup(ast);
      var typechecker = new TypeChecker();
      typechecker.verify(ast);
      var lowering = new ViamLowering();
      var spec = lowering.generate(ast);
      ViamVerifier.verifyAllIn(spec);
    } catch (DiagnosticList d) {
      diagnostics = d.items;
    } catch (Diagnostic d) {
      diagnostics = List.of(d);
    }

    // FIXME: Maybe deferred diagnostic store here but add later because it might have problems
    // being global state not reset by the test suite.

    // Force relative paths because the tests must always produce the same and the absolut path
    // will differ on different machines.
    DiagnosticPrinter printer = new DiagnosticPrinter(false);
    printer.forceRelativePaths = true;
    var output = !diagnostics.isEmpty() ? printer.toString(diagnostics).stripTrailing() :
        "No diagnostics were reported, the input was correctly parsed, typechecked and lowered.";
    output = "//  " + output.replaceAll("\n", "\n//  ") + "\n//\n//\n// Part of the %s".formatted(
        this.getClass());

    var input = Files.readString(path);
    var stripped = commentPattern.matcher(input).replaceAll("").strip();
    var actual = stripped + "\n\n\n// Reported Diagnostics:\n//\n" + output;


    if (System.getenv("UPDATE_SNAPSHOTS") != null) {
      Files.writeString(path, actual);
      return;
    }

    assertEqualsFileContent(path, actual);
  }
}
