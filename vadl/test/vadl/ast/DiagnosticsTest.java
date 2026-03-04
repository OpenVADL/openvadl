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

import static vadl.TestUtils.assertEqualsFileLines;
import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.error.DiagnosticPrinter;
import vadl.utils.DiskVirtualFileSystem;
import vadl.viam.passes.verification.ViamVerifier;

/// Runs all files in the test/resources/diagnostics directory.
/// The files are fed into the compilation pipeline and the thrown diagnostics are stored.
/// Then the original file is read again and compared if it reported the same diagnostics.
///
/// To update the snapshots, set the environment variable UPDATE_SNAPSHOTS.
/// `UPDATE_SNAPSHOTS=true ./gradlew test --tests vadl.ast.DiagnosticsTest`
///
/// There are some configurations you can specify in the testfiles that change the output. These
/// must be included in the file as a single line comment by itself. It can also not be part of the
/// large block at the bottom and the convention is to include them at the top. Here are some
/// examples:
/// - `INCLUDE-AST-DUMP` will also insert the whole AST dump into the
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
      Pattern.compile("//  Reported Diagnostics:.*$", Pattern.DOTALL);
  private Pattern includeAstDumpPattern =
      Pattern.compile("^// *INCLUDE-AST-DUMP *$", Pattern.MULTILINE);

  void runSnapshotTest(Path path) throws IOException {

    @Nullable Ast ast = null;
    List<Diagnostic> diagnostics = List.of();
    try {
      var result = Frontend.compileToAstAndViam(path, new DiskVirtualFileSystem());
      ast = result.left();
      var spec = result.right();
      ViamVerifier.verifyAllIn(spec);
      ViamLocationExistenceChecker.verify(spec);

      // Some additional checks
      verifyPrettifiedAst(ast);
    } catch (DiagnosticList d) {
      diagnostics = d.items;
    } catch (Diagnostic d) {
      diagnostics = List.of(d);
    }

    // FIXME: Maybe deferred diagnostic store here but add later because it might have problems
    // being global state not reset by the test suite.

    // Force relative and slashified paths because the tests must always produce the same and the
    // absolute path and file separator will differ on different machines.
    DiagnosticPrinter printer = new DiagnosticPrinter(new DiskVirtualFileSystem(), false);
    printer.forceRelativePaths = true;
    printer.forceUnixPaths = true;
    var output = "Reported Diagnostics:\n\n";
    output += !diagnostics.isEmpty() ? printer.toString(diagnostics).stripTrailing() :
        "No diagnostics were reported, the input was correctly parsed, typechecked and lowered.";


    var input = Files.readString(path);
    if (includeAstDumpPattern.matcher(input).find()) {
      output += "\n\n\nDumped AST:\n\n";
      if (ast != null) {
        output += new AstDumper().dump(ast).indent(2);
      } else {
        output += "Unable to dump AST";
      }
    }

    output = "//  "
        + output.strip().replaceAll("\n", "\n//  ").replaceAll("// +\n", "//\n")
        + "\n//\n//\n// Part of the %s".formatted(
        this.getClass());

    var stripped = commentPattern.matcher(input).replaceAll("").strip();
    var actual = stripped + "\n\n\n" + output;


    if (System.getenv("UPDATE_SNAPSHOTS") != null) {
      Files.writeString(path, actual);
      return;
    }

    assertEqualsFileLines(path, actual);
  }

  // Similar to the full version but it infers the name from the method name of the test.
  static void convertTest(String prog, String folder, Boolean isNegative,
                          Boolean includeAst) {
    String name = Thread.currentThread().getStackTrace()[2].getMethodName();
    convertTest(prog, name, folder, isNegative, includeAst);
  }

  /// This is just a helper method that can be called in older tests to Diagnostic tests.
  /// You just have to call it in the original test and it will create a matching diagnostic test.
  static void convertTest(String prog, String name, String folder, Boolean isNegative,
                          Boolean includeAst) {
    String dir = "test/resources/diagnostics/" + folder;
    String fileName = name.replaceAll("Text$", "") + ".vadl";
    if (isNegative) {
      fileName = "invalid" + fileName.substring(0, 1).toUpperCase() + fileName.substring(1);
    }
    Path path = Paths.get(dir, fileName);

    String content = "";
    if (includeAst) {
      content += "// INCLUDE-AST-DUMP\n\n";
    }
    content += prog.stripIndent();

    if (Files.exists(path)) {
      throw new RuntimeException("File `%s` was already generated".formatted(path.toString()));
    }

    try {
      Files.writeString(path, content);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }
}
