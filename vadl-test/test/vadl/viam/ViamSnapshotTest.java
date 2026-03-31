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

package vadl.viam;

import static vadl.TestUtils.assertEqualsFileLines;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.TestUtils;

/**
 * Snapshot tests for VIAM dumps.
 *
 * <p>To update snapshots:
 * {@code UPDATE_SNAPSHOTS=true ./gradlew test --tests vadl.viam.ViamSnapshotTest}</p>
 */
class ViamSnapshotTest {
  private static final Pattern DUMP_TAIL_PATTERN =
      Pattern.compile("//  Dumped VIAM:.*$", Pattern.DOTALL);
  private static final Pattern INCLUDE_VIAM_DUMP_PATTERN =
      Pattern.compile("^// *INCLUDE-VIAM-DUMP *$", Pattern.MULTILINE);

  @TestFactory
  Stream<DynamicTest> snapshotTests() throws IOException {
    return Files.walk(Paths.get("resources/viam-snapshots"))
        .filter(path -> path.toString().endsWith(".vadl"))
        .map(path -> DynamicTest.dynamicTest(path.toString(), () -> runSnapshotTest(path)));
  }

  private void runSnapshotTest(Path path) throws IOException {
    var input = Files.readString(path);
    if (!INCLUDE_VIAM_DUMP_PATTERN.matcher(input).find()) {
      throw new IllegalStateException(
          "Snapshot fixture must include // INCLUDE-VIAM-DUMP: " + path);
    }

    var spec = TestUtils.compileToViam(input);
    var dump = new ViamSnapshotDumper().dump(spec);

    var output = "Dumped VIAM:\n\n" + dump.indent(2);
    output = "//  "
        + output.strip().replaceAll("\n", "\n//  ").replaceAll("// +\n", "//\n")
        + "\n//\n//\n// Part of the %s".formatted(this.getClass());

    var stripped = DUMP_TAIL_PATTERN.matcher(input).replaceAll("").strip();
    var actual = stripped + "\n\n\n" + output;

    if (System.getenv("UPDATE_SNAPSHOTS") != null) {
      Files.writeString(path, actual);
      return;
    }

    assertEqualsFileLines(path, actual);
  }
}
