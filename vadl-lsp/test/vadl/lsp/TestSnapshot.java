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

package vadl.lsp;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.StackWalker.StackFrame;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.FileInfo;

/// Snapshot of a Test.
///
/// How to use:
/// - In the unit test, use [#add(String, String)] to continuously add relevant input and
///   result data to this snapshot.
/// - At the end, call [#verify()]: This will compare all collected snapshot data with the
///   state stored in a file and fail or pass the test accordingly.
/// - If the snapshot file does not exist yet, it is created automatically. You must verify its
///   content makes sense, or the test is meaningless.
/// - To update snapshots, simply delete the snapshot file, or set the environment variable UPDATE
///   SNAPSHOTS: `UPDATE_SNAPSHOTS=true ./gradlew :vadl-lsp:test`
/// - Note that the test fails if the snapshot file is (re-)generated. This is intended to point out
///   that the snapshot has changed and the file should be inspected. The test will pass on the next
///   invocation.
public class TestSnapshot {
  private static final String BASE_PATH = "test/resources/snapshots";
  private static final String EXTENSION = ".dat";

  private static final StackWalker stackWalker = StackWalker.getInstance(Set.of(), 3);
  private static final boolean UPDATE_SNAPSHOTS = System.getenv("UPDATE_SNAPSHOTS") != null;

  private final StringBuilder data = new StringBuilder();


  /**
   * Adds data to this snapshot.
   *
   * @param name A meaningful name for the given data
   * @param value The data to record. Effectively, the String representation of this data is
   *              recorded in the snapshot, therefore it should be meaningful to a developer
   *              verifying the snapshot data.
   */
  public void add(String name, Object value) {
    data.append("- ").append(name).append(": ");

    var valueString = value.toString();
    if (valueString.contains("\n")) {
      data.append("\n  ");
      data.append(valueString.replace("\n", "\n  "));
      data.append("\n");
    } else {
      data.append(value);
    }
    data.append("\n");
  }

  public void addNote(String note) {
    data.append("# ").append(note.replace("\n", "\n# ")).append("\n");
  }

  /**
   * Call this at the end of the test. Verifies the snapshot data. I.e. test fails if data recorded
   * in this snapshot object differs from the data stored in the snapshot file.
   *
   * <p>Note: The snapshot is stored under the name of the method calling <i>this</i> method.
   */
  public void verify() {
    var callerData = getCallerData();

    try {
      verify(callerData.getClassName(), callerData.getMethodName());
    } catch (IOException e) {
      fail(e);
    }
  }

  /**
   * Internal implementation of {@link #verify()}.
   *
   * @param testClass fully-qualified class name
   * @param testMethod relative to {@code testClass}
   */
  private void verify(String testClass, String testMethod) throws IOException {
    String finalSnapshot = getFileHeader(testClass, testMethod) + data;

    var directoryPath = Paths.get(BASE_PATH, testClass);
    var filePath = directoryPath.resolve(testMethod + EXTENSION);
    Files.createDirectories(directoryPath);

    if (!Files.exists(filePath)) {
      Files.createFile(filePath);
      Files.writeString(filePath, finalSnapshot);

      fail(
          "Generated Test snapshot for " + testClass + "." + testMethod
              + "\n  Please verify if snapshot data in this file is valid: " + filePath.toUri()
              + "\n  This test will pass on the next invocation.");
    }

    if (UPDATE_SNAPSHOTS) {
      try {
        assertEqualsFile(filePath, finalSnapshot);
      } catch (AssertionFailedError e) {
        Files.writeString(filePath, finalSnapshot);
        fail(
            "Updated Test snapshot for " + testClass + "." + testMethod
                + "\n  Please verify if snapshot data in this file is valid: " + filePath.toUri()
                + "\n  This test will pass on the next invocation.");
      }
      return;
    }

    assertEqualsFile(filePath, finalSnapshot);
  }

  private StackFrame getCallerData() {
    return stackWalker.walk(s -> s.skip(2).findFirst())
        .orElseThrow();
  }

  private String getFileHeader(String testClass, String testMethod) {
    return "# This file contains a test snapshot, generated by " + getClass().getName()
        + "\nTest method: " + testClass + "." + testMethod + "\n\n";
  }

  /**
   * Asserts that the content of the File referenced with the provided Path equals the actual
   * string.
   *
   * <p>This test is optimized for IntelliJ and by throwing a custom AssertionFailedError the IDE
   * will show a diff editor in which the user can accept the changes into the referenced file.
   *
   * @param expectedPath the file to compare the actual string with
   * @param actual       the actual string the test produced
   * @throws IOException if the file cannot be read
   * @throws AssertionFailedError if assertion fails
   */
  private void assertEqualsFile(Path expectedPath, String actual) throws IOException {
    var expected = Files.readString(expectedPath);
    if (!expected.equals(actual)) {
      throw new AssertionFailedError(
          "Actual data differs from snapshot file: " + expectedPath.toUri(),
          new FileInfo(expectedPath.toAbsolutePath().toString(),
              expected.getBytes(StandardCharsets.UTF_8)),
          actual
      );
    }
  }
}
