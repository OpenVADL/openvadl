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

package vadl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.FileInfo;

/**
 * Frontend test utilities.
 */
public class TestUtils {

  /**
   * Asserts that the file referenced by the provided path contains the actual string.
   */
  public static void assertEqualsFileLines(Path expectedPath, String actual) throws IOException {
    var expected = Files.readString(expectedPath);
    if (!expected.lines().toList().equals(actual.lines().toList())) {
      throw new AssertionFailedError(
          "Actual data differs from file.",
          new FileInfo(expectedPath.toAbsolutePath().toString(),
              expected.getBytes(StandardCharsets.UTF_8)),
          actual
      );
    }
  }
}
