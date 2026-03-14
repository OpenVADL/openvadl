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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A singleton implementation to keep the reference to the generated docker image to avoid
 * recompilation between {@link LlvmRiscvAssemblyTest} and {@link SpikeRiscvSimulationTest}.
 */
public class DockerRiscvImageProvider {
  private static Map<String, String> images = new HashMap<>();

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
   * @param doDebug             if the flag is {@code true} then the image will not be deleted.
   * @throws RuntimeException when the {@code isCI} environment variable and {@code doDebug} are
   *                          activated.
   */
  public static String image(String pathDockerFile,
                             String imageName,
                             String target,
                             String upstreamBuildTarget,
                             String upstreamClangTarget,
                             String spikeTarget,
                             String abi,
                             boolean doDebug) throws IOException {
    var image = images.get(imageName);
    if (image == null) {

      var deleteOnExit = !doDebug;

      if ("true".equals(System.getenv("isCI")) && !deleteOnExit) {
        throw new RuntimeException("It is not allowed to activate 'deleteOnExit' in the CI");
      }

      try (var client = new BuildkitClient(
          BuildkitConnectionConfig.of("tcp://localhost:1234").withTimeout(
              Duration.ofHours(2)))) {
        var requestBuilder = DockerfileBuildRequest.builder(
                Path.of(pathDockerFile).getParent(),
                Path.of(pathDockerFile),
                "tc_spike_riscv_" + imageName
            )
            .buildArg("TARGET", target)
            .buildArg("UPSTREAM_BUILD_TARGET", upstreamBuildTarget)
            .buildArg("UPSTREAM_CLANG_TARGET", upstreamClangTarget)
            .buildArg("ABI", abi)
            .buildArg("SPIKE_TARGET", spikeTarget)
            .outputMode(BuildOutputMode.DOCKER);

        BuildResult result = client.buildImage(requestBuilder.build(), new PrintingListener());

        loadIntoDocker(result);

        images.put(imageName, result.imageDigest());
        return result.imageDigest();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } else {
      return image;
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
