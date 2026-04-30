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

package vadl.iss;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testcontainers.utility.MountableFile;
import vadl.utils.VadlFileUtils;

public abstract class IssEmbenchBenchmark extends QemuIssTest {

  private static final String DEFAULT_BENCHMARK_OUTPUT_DIR = "/work/iss-benchmark-results";
  private static final String BENCHMARK_RESULTS_HOST_DIR_ENV = "ISS_BENCHMARK_RESULT_HOST_DIR";
  private static final String PROJECT_ROOT_ENV = "PROJECT_ROOT";
  private static final String DEFAULT_HOST_RESULTS_DIR = "vadl/build/iss-benchmarks";

  protected record BenchmarkSpec(
      String resultKey,
      String vadlPath,
      String runnerScript,
      String normalizedResultsDir
  ) {
  }

  protected BenchmarkSpec benchmarkSpec(
      String resultKey,
      String vadlPath,
      String runnerScript,
      String normalizedResultsDir
  ) {
    return new BenchmarkSpec(resultKey, vadlPath, runnerScript, normalizedResultsDir);
  }

  protected void runBenchmark(BenchmarkSpec benchmark) throws IOException {
    var image = generateIssSimulator(benchmark.vadlPath());
    var embenchPath = VadlFileUtils.copyResourceDirToTempDir("embench", "embench");
    var hostResultsDir = resolveHostResultsDir();
    var guestOutputDir = DEFAULT_BENCHMARK_OUTPUT_DIR + "/" + benchmark.resultKey();

    var runCommand = String.join(" && ",
        "chmod -R +x /work/embench",
        "cd /work/embench",
        "bash ./" + benchmark.runnerScript(),
        "rm -rf " + guestOutputDir,
        "mkdir -p " + guestOutputDir,
        "cp -r ./benchmark-extras/results " + guestOutputDir + "/raw-results",
        "cp -r ./" + benchmark.normalizedResultsDir() + " " + guestOutputDir + "/normalized-results"
    );

    runContainer(image,
        container -> container
            .withCopyFileToContainer(MountableFile.forHostPath(embenchPath), "/work/embench")
            .withCommand("/bin/bash", "-c", runCommand),
        container -> {
          try {
            var outputPath = hostResultsDir.resolve(benchmark.resultKey());
            Files.createDirectories(outputPath.getParent());
            copyPathFromContainer(container, guestOutputDir, outputPath);
          } catch (IOException e) {
            throw new RuntimeException("Failed to copy ISS benchmark results to host", e);
          }
        });
  }

  private Path resolveHostResultsDir() {
    var configuredDir = System.getenv(BENCHMARK_RESULTS_HOST_DIR_ENV);
    if (configuredDir != null && !configuredDir.isBlank()) {
      return Path.of(configuredDir);
    }

    var projectRoot = System.getenv(PROJECT_ROOT_ENV);
    if (projectRoot == null || projectRoot.isBlank()) {
      throw new IllegalStateException(
          "Neither " + BENCHMARK_RESULTS_HOST_DIR_ENV + " nor " + PROJECT_ROOT_ENV + " is set.");
    }
    return Path.of(projectRoot, DEFAULT_HOST_RESULTS_DIR);
  }
}
