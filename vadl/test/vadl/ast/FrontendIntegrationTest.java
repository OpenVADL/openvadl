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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.shaded.org.checkerframework.checker.nullness.qual.Nullable;

public class FrontendIntegrationTest {

  @ParameterizedTest
  @ValueSource(strings = {
      "../sys/risc-v/rv32i.vadl",
      "../sys/risc-v/rv64i.vadl",
      "../sys/risc-v/rv32im.vadl",
      "../sys/risc-v/rv64im.vadl",
      "../sys/risc-v/rvcsr.vadl",
      "../sys/aarch64/aarch64.vadl"
  })
  public void testFrontendPassingOnSysSpecs(String filename) {
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(Path.of(filename)),
        "Cannot parse input");
    new Ungrouper().ungroup(ast);
    new ModelRemover().removeModels(ast);
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var lowering = new ViamLowering();
    Assertions.assertDoesNotThrow(() -> lowering.generate(ast), "Cannot generate VIAM");
  }

  @Test
  void testRv32im() throws IOException {
    testPrettyPrintSpec(Path.of("../sys/risc-v"), "rv3264im.vadl", "rv32im.vadl");
  }

  private void testPrettyPrintSpec(Path rootDir, String... vadlSpecs) throws IOException {
    var absRoot = rootDir.toAbsolutePath();
    var sysName = rootDir.getFileName();
    var prettyPath = Path.of("build/test/pretty-print/" + sysName);
    if (prettyPath.toFile().exists()) {
      FileUtils.forceDelete(prettyPath.toFile());
    }

    var specFiles = Arrays.stream(vadlSpecs).map(s -> absRoot.resolve(s).toFile())
        .toList();
    var prettyFiles =
        Arrays.stream(vadlSpecs).map(s -> prettyPath.resolve(s).toFile())
            .toList();

    // first check all handwirtten specs
    checkAll(specFiles, prettyPath);
    // then check all pretty printed specs
    checkAll(prettyFiles, null);
  }

  private void checkAll(List<File> specs, @Nullable Path prettyPrintPath) throws IOException {
    for (var file : specs) {
      check(file, prettyPrintPath);
    }
  }

  private void check(File vadlFile, @Nullable Path prettyPrintPath) throws IOException {
    System.out.println("Checking " + vadlFile);
    assertThat(vadlFile).exists();

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(vadlFile.toPath()),
        "Cannot parse input");
    new Ungrouper().ungroup(ast);
    new ModelRemover().removeModels(ast);
    if (prettyPrintPath != null) {
      var progPretty = ast.prettyPrintToString();
      var resFile = prettyPrintPath.resolve(vadlFile.getName()).toFile();
      FileUtils.createParentDirectories(resFile);
      FileUtils.writeStringToFile(resFile, progPretty, Charset.defaultCharset());
    }
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var lowering = new ViamLowering();
    Assertions.assertDoesNotThrow(() -> lowering.generate(ast), "Cannot generate VIAM");
  }
}
