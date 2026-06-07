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

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.StackWalker.StackFrame;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.FileInfo;

/// Snapshot of a Test.
///
/// How to use:
/// - In the unit test, use [#add(String, Object)] to continuously add relevant input and
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
/// - Any kind of test input can be loaded from input files that are stored alongside the snapshot
///   file.
/// - If you have a parameterized test where each test case should have different input and snapshot
///   files, the name of the current test case can be provided.
public class TestSnapshot {
  private static final String BASE_PATH = "test/resources/snapshots";
  private static final String EXTENSION = ".snapshot";

  private static final StackWalker stackWalker = StackWalker.getInstance(Set.of(), 3);
  private static final boolean UPDATE_SNAPSHOTS = System.getenv("UPDATE_SNAPSHOTS") != null;

  /**
   * fully-qualified class name.
   */
  private final String testClass;
  /**
   * testMethod relative to {@link #testClass}.
   */
  private final String testMethod;
  @Nullable
  private final String testCase;
  private final boolean shortenFilePath;

  private final StringBuilder data = new StringBuilder();
  @Nullable
  private Path containingDirectoryPath = null;


  /**
   * Creates a new instance.
   *
   * <p>Note: The snapshot is stored under the name of the method that instantiates the Snapshot
   * object.
   */
  public TestSnapshot() {
    var callerData = getCallerData();
    this.testClass = callerData.getClassName();
    this.testMethod = callerData.getMethodName();

    this.testCase = null;
    this.shortenFilePath = false;
  }

  /**
   * Creates a new instance.
   *
   * <p>Note: The snapshot is stored under the name of the method that instantiates the Snapshot
   * object.
   *
   * @param testCase If not null: In addition to test class and test method, this test case is used
   *                 to identify this snapshot. Use this for ParameterizedTests.
   */
  public TestSnapshot(@Nullable String testCase) {
    var callerData = getCallerData();
    this.testClass = callerData.getClassName();
    this.testMethod = callerData.getMethodName();

    this.testCase = testCase;
    this.shortenFilePath = false;
  }

  /**
   * Creates a new instance.
   *
   * <p>Note: The snapshot is stored under the name of the method that instantiates the Snapshot
   * object (unless {@code shortenFilePath} is true).
   *
   * @param testCase In addition to test class and test method, this test case is used to identify
   *                 this snapshot. Use this for ParameterizedTests.
   * @param shortenFilePath True: Do not include the method name as part of file paths. Should not
   *                        be used if the test class has more than one test method that uses
   *                        snapshots.
   */
  public TestSnapshot(String testCase, boolean shortenFilePath) {
    var callerData = getCallerData();
    this.testClass = callerData.getClassName();
    this.testMethod = callerData.getMethodName();

    this.testCase = testCase;
    this.shortenFilePath = shortenFilePath;
  }


  /**
   * Adds data to this snapshot.
   *
   * @param name A meaningful name for the given data
   * @param value The data to record. Effectively, the String representation of this data is
   *              recorded in the snapshot, therefore it should be meaningful to a developer
   *              verifying the snapshot data.
   */
  public void add(String name, @Nullable Object value) {
    data.append("- ").append(name).append(": ");

    var valueString = Objects.toString(value);
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
   * Provides the path of the input file with the given name. If the file doesn't exist yet, it is
   * created and the test fails.
   *
   * @param name relative to this snapshot's test class and method
   */
  public Path getInputPath(String name) {
    data.append("# % Requested path of input file").append(name).append(" %\n");
    return requireNonNull(createInputPath(name));
  }

  private Path createInputPath(String name) {
    try {
      var filePath = getFilePath("-" + name);

      if (!Files.exists(filePath)) {
        Files.createFile(filePath);
        fail(
            "Generated input file " + name + " for " + testClass + "." + testMethod
                + (testCase != null ? "(" + testCase + ")" : "")
                + "\n  Please fill this file with input data as desired: " + filePath.toUri()
                + "\n  This test will not fail here on the next invocation.");
      }
      return filePath;
    } catch (IOException e) {
      fail(e);
      return null;
    }
  }

  /**
   * Provides the URI of the input file with the given name. If the file doesn't exist yet, it is
   * created and the test fails.
   *
   * @param name relative to this snapshot's test class and method
   */
  public String getInputUri(String name) {
    data.append("# % Requested uri of input file ").append(name).append(" %\n");
    return requireNonNull(createInputPath(name)).toUri().toString();
  }

  /**
   * Returns the name of an input file based on the given full uri. If the uri points to a file that
   * couldn't be an input file of this snapshot, the test fails.
   *
   * @param uri e.g. the result of {@link #getInputUri(String)}
   */
  public String getInputNameFromUri(String uri) {
    try {
      var prefix = getFilePath("-").toUri().toString();
      if (!uri.startsWith(prefix)) {
        fail("Given uri does not point to an input file of this snapshot: " + uri);
      }
      return uri.substring(prefix.length());
    } catch (IOException e) {
      fail(e);
      return null;
    }
  }

  /**
   * Provides the content of the input file with the given name. If the file doesn't exist yet, it
   * is created and the test fails.
   *
   * @param name relative to this snapshot's test class and method
   */
  public String getInputData(String name) {
    data.append("# % Requested data of input file ").append(name).append(" %\n");
    try {
      return Files.readString(requireNonNull(createInputPath(name)));
    } catch (IOException e) {
      fail(e);
      return "";
    }
  }

  /**
   * Call this at the end of the test. Verifies the snapshot data. I.e. test fails if data recorded
   * in this snapshot object differs from the data stored in the snapshot file.
   */
  public void verify() {
    try {
      String finalSnapshot = getFileHeader() + data;
      var filePath = getFilePath(EXTENSION);

      if (!Files.exists(filePath)) {
        Files.createFile(filePath);
        Files.writeString(filePath, finalSnapshot);

        failOnFileChange("Generated",  filePath);
      }

      if (UPDATE_SNAPSHOTS) {
        try {
          assertEqualsFile(filePath, finalSnapshot);
        } catch (AssertionFailedError e) {
          Files.writeString(filePath, finalSnapshot);
          failOnFileChange("Updated", filePath);
        }
        return;
      }

      assertEqualsFile(filePath, finalSnapshot);
    } catch (IOException e) {
      fail(e);
    }
  }


  private StackFrame getCallerData() {
    return stackWalker.walk(s -> s.skip(2).findFirst())
        .orElseThrow();
  }

  private Path getAndCreateContainingDirectoryPath() throws IOException {
    if (containingDirectoryPath == null) {
      containingDirectoryPath = Paths.get(BASE_PATH, testClass);
      Files.createDirectories(containingDirectoryPath);
    }

    return containingDirectoryPath;
  }

  private Path getFilePath(String afterPrefix) throws IOException {
    var directoryPath = getAndCreateContainingDirectoryPath();

    String relativePath = (!shortenFilePath
        ? testMethod + (testCase != null ? "-" + testCase : "")
        : (testCase != null ? testCase : ""))
        + afterPrefix;
    var filePath = directoryPath.resolve(relativePath);

    if (!filePath.getParent().equals(directoryPath)) {
      throw new IllegalArgumentException(
          "Must not create file path that lies outside of snapshot directory, relative path is: "
              + relativePath);
    }
    return filePath;
  }

  private String getFileHeader() {
    return "# This file contains a test snapshot, generated by " + getClass().getName()
        + "\nTest method: " + testClass + "." + testMethod
        + (testCase != null ? ", Test case: " + testCase : "") + "\n\n";
  }

  private void failOnFileChange(String actionDone, Path filePath) {
    fail(
        actionDone + " Test snapshot for " + testClass + "." + testMethod
            + (testCase != null ? "(" + testCase + ")" : "")
            + "\n  Please verify if snapshot data in this file is valid: " + filePath.toUri()
            + "\n  This test will pass on the next invocation.");
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
