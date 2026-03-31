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

package vadl.lcb.riscv;

import io.github.kper.buildkitcli.lib.BuildLog;
import io.github.kper.buildkitcli.lib.BuildOutputMode;
import io.github.kper.buildkitcli.lib.BuildProgressListener;
import io.github.kper.buildkitcli.lib.BuildResult;
import io.github.kper.buildkitcli.lib.BuildVertex;
import io.github.kper.buildkitcli.lib.BuildWarning;
import io.github.kper.buildkitcli.lib.BuildkitClient;
import io.github.kper.buildkitcli.lib.BuildkitConnectionConfig;
import io.github.kper.buildkitcli.lib.DockerfileBuildRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.DockerBuildKitDaemon;

/**
 * A singleton implementation to keep the reference to the generated docker image to avoid
 * recompilation between {@link LlvmRiscvAssemblyTest} and {@link SpikeRiscvSimulationTest}.
 */
public class DockerRiscvImageProvider {
  private static final Logger logger = LoggerFactory.getLogger(DockerRiscvImageProvider.class);
  public static HashSet<String> images = new HashSet<>();

  static class PrintingListener implements BuildProgressListener {
    private final Logger logger = LoggerFactory.getLogger(PrintingListener.class);

    @Override
    public void onVertex(BuildVertex vertex) {
      if (!vertex.name().isBlank()) {
        logger.info(vertex.name());
      }
    }

    @Override
    public void onLog(BuildLog log) {
      String message = log.utf8Message();
      if (!message.isBlank()) {
        logger.info(message);
      }
    }

    @Override
    public void onWarning(BuildWarning warning) {
      logger.warn("warning: {}", warning.shortMessage());
    }
  }


  /**
   * Create a docker image or return an already existing image.
   *
   * @param pathDockerFile      is the path to the dockerfile which should be built.
   * @param imageName           is the name of the generated and cached docker image
   * @param target              is the name of the processor.
   * @param upstreamBuildTarget is the name of LLVM backend to compile an upstream compiler.
   * @param upstreamClangTarget is the name for the LLVM clang option to invoke the upstream
   *                            compiler.
   * @param spikeTarget         is the ISA for spike to run the executable.
   * @param abi                 which should be chosen for the gcc linker.
   * @return the name of the image.
   * @throws RuntimeException when the {@code isCI} environment variable and {@code doDebug} are
   *                          activated.
   */
  public static String image(String pathDockerFile,
                             String imageName,
                             String target,
                             String upstreamBuildTarget,
                             String upstreamClangTarget,
                             String spikeTarget,
                             String abi) throws IOException {
    var imageKey = "tc_spike_riscv_" + imageName;
    var image = images.contains(imageKey);
    if (!image) {

      var daemon = DockerBuildKitDaemon.getRunningInstance();
      var port = daemon.getMappedPort(DockerBuildKitDaemon.BUILDKIT_DAEMON_PORT);
      try (var client = new BuildkitClient(
          BuildkitConnectionConfig.of("tcp://localhost:" + port)
              .withTimeout(Duration.ofHours(2)))) {
        Path dockerfile = Path.of(pathDockerFile);
        var requestBuilder = DockerfileBuildRequest.builder(
                dockerfile.getParent(),
                dockerfile,
                imageKey
            )
            .buildArg("TARGET", target)
            .buildArg("UPSTREAM_BUILD_TARGET", upstreamBuildTarget)
            .buildArg("UPSTREAM_CLANG_TARGET", upstreamClangTarget)
            .buildArg("ABI", abi)
            .buildArg("SPIKE_TARGET", spikeTarget)
            .outputMode(BuildOutputMode.DOCKER);

        BuildResult result = client.buildImage(requestBuilder.build(), new PrintingListener());
        logger.info("Image was built: {}", imageKey);
        logger.info("Loading image: {}", imageKey);
        loadIntoDocker(result);

        images.add(imageKey);
        return imageKey;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } else {
      logger.info("Image was already in the cache: {}", imageKey);
      return imageKey;
    }
  }

  private static void loadIntoDocker(BuildResult result) throws Exception {
    Path exportedArchive = result.exportedArchive();
    if (exportedArchive == null) {
      throw new IllegalStateException("Build result did not include a docker archive");
    }
    try {
      Process process = new ProcessBuilder("docker", "load", "-i", exportedArchive.toString())
          .inheritIO()
          .start();
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new IllegalStateException("docker load failed with exit code " + exitCode);
      }
    } finally {
      Files.deleteIfExists(exportedArchive);
    }
  }
}
