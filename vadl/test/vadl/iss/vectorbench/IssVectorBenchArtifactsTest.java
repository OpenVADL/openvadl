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

package vadl.iss.vectorbench;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.AbstractTest;

class IssVectorBenchArtifactsTest extends AbstractTest {

  private static final Logger log = LoggerFactory.getLogger(IssVectorBenchArtifactsTest.class);

  /**
   * Verifies that the committed benchmark artifacts under {@code vadl-test/resources/vectorbench64}
   * match what the current generator produces.
   *
   * <p>If this test fails, run {@code ./gradlew :vadl:benchmark-iss-vectorbench64} locally and
   * commit the updated files under {@code vadl-test/resources/vectorbench64}.
   */
  @Test
  void vectorBench64CommittedArtifactsAreUpToDate() throws IOException {
    var committedDir = resolveProjectPath("vadl-test/resources/vectorbench64");
    var tempDir = Files.createTempDirectory("vectorbench64-check");
    try {
      log.info("Generating benchmark artifacts in {} ...", tempDir);
      VectorBench64Benchmarks.generate(tempDir, null);
      log.info("done.");

      var mismatches = new ArrayList<String>();

      log.info("Checking for differences between generated and committed artifacts ...");
      try (var stream = Files.walk(tempDir)) {
        stream.filter(Files::isRegularFile).forEach(generated -> {
          log.info("Checking {}", generated);
          var rel = tempDir.relativize(generated);
          var committed = committedDir.resolve(rel);
          try {
            if (!Files.exists(committed)) {
              mismatches.add("missing from repo: " + rel);
            } else if (!Arrays.equals(Files.readAllBytes(generated),
                Files.readAllBytes(committed))) {
              mismatches.add("content differs: " + rel);
            }
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
      }

      log.info("Checking for stale artifacts ...");
      if (Files.exists(committedDir)) {
        try (var stream = Files.walk(committedDir)) {
          stream.filter(Files::isRegularFile)
              .filter(p -> {
                var name = p.getFileName().toString();
                return name.endsWith(".elf") || name.equals("manifest.csv");
              })
              .forEach(committed -> {
                var rel = committedDir.relativize(committed);
                if (!Files.exists(tempDir.resolve(rel))) {
                  mismatches.add("stale in repo (no longer generated): " + rel);
                }
              });
        }
      }

      log.info("done.");

      if (!mismatches.isEmpty()) {
        Assertions.fail(
            "Committed benchmark artifacts under vadl-test/resources/vectorbench64 are stale.\n"
                + "Run ./gradlew :vadl:benchmark-iss-vectorbench64 and commit the changes.\n"
                + String.join("\n", mismatches));
      }
    } finally {
      try (var stream = Files.walk(tempDir)) {
        stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
  }

  /**
   * Resolves absolute paths directly and repository-relative paths against {@code PROJECT_ROOT}.
   */
  private Path resolveProjectPath(String value) {
    var path = Path.of(value);
    if (path.isAbsolute()) {
      return path.normalize();
    }

    var projectRoot = System.getenv("PROJECT_ROOT");
    if (projectRoot == null || projectRoot.isBlank()) {
      throw new IllegalStateException(
          "PROJECT_ROOT not set is set.");
    }
    return Path.of(projectRoot).resolve(path).normalize();
  }
}
