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

import io.github.kper.buildkitcli.lib.BuildLog;
import io.github.kper.buildkitcli.lib.BuildOutputMode;
import io.github.kper.buildkitcli.lib.BuildProgressListener;
import io.github.kper.buildkitcli.lib.BuildResult;
import io.github.kper.buildkitcli.lib.BuildVertex;
import io.github.kper.buildkitcli.lib.BuildWarning;
import io.github.kper.buildkitcli.lib.BuildkitClient;
import io.github.kper.buildkitcli.lib.BuildkitConnectionConfig;
import io.github.kper.buildkitcli.lib.BuildkitException;
import io.github.kper.buildkitcli.lib.DockerfileBuildRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.dockerfile.DockerfileBuilder;

/**
 * A small test helper that preserves the subset of the {@code ImageFromDockerfile} API used in
 * this codebase while delegating image builds to buildkit.
 */
public final class DockerImage {

  private static final Logger logger = LoggerFactory.getLogger(DockerImage.class);
  private static final BuildkitConnectionConfig BUILDKIT_CONFIG =
      BuildkitConnectionConfig.of("tcp://localhost:1234");

  private final String imageName;
  private final Map<String, Path> filesFromPath = new LinkedHashMap<>();
  private final Map<String, String> filesFromClasspath = new LinkedHashMap<>();
  private final Map<String, String> buildArgs = new LinkedHashMap<>();

  private Path dockerfilePath;
  private String dockerfileContents;
  private String resolvedImageName;

  public DockerImage() {
    this("vadl-test-" + UUID.randomUUID());
  }

  public DockerImage(String imageName) {
    this.imageName = imageName;
  }

  public DockerImage withDockerfile(Path path) {
    this.dockerfilePath = path.toAbsolutePath().normalize();
    this.dockerfileContents = null;
    return this;
  }

  public DockerImage withDockerfile(String content) {
    this.dockerfilePath = null;
    this.dockerfileContents = content;
    return this;
  }

  @Deprecated
  public DockerImage withDockerfileFromBuilder(Consumer<DockerfileBuilder> consumer) {
    var builder = new DockerfileBuilder();
    consumer.accept(builder);
    this.dockerfileContents = builder.build();
    this.dockerfilePath = null;
    return this;
  }

  public DockerImage withFileFromPath(String path, Path source) {
    filesFromPath.put(normalizeContextPath(path), source.toAbsolutePath().normalize());
    return this;
  }

  public DockerImage withFileFromClasspath(String path, String resourcePath) {
    filesFromClasspath.put(normalizeContextPath(path), resourcePath);
    return this;
  }

  public DockerImage withBuildArg(String key, String value) {
    buildArgs.put(key, value);
    return this;
  }

  public synchronized String getDockerImageName() {
    if (resolvedImageName != null) {
      return resolvedImageName;
    }

    Path tempContext = null;
    try {
      Path contextDir;
      Path dockerfile;

      if (dockerfileContents != null) {
        tempContext = Files.createTempDirectory("buildkit-context-");
        stageSupplementalFiles(tempContext);
        dockerfile = tempContext.resolve("Dockerfile");
        Files.writeString(dockerfile, dockerfileContents);
        contextDir = tempContext;
      } else if (dockerfilePath != null) {
        if (filesFromPath.isEmpty() && filesFromClasspath.isEmpty()) {
          dockerfile = dockerfilePath;
          contextDir = dockerfilePath.getParent();
        } else {
          tempContext = Files.createTempDirectory("buildkit-context-");
          FileUtils.copyDirectory(dockerfilePath.getParent().toFile(), tempContext.toFile());
          stageSupplementalFiles(tempContext);
          dockerfile = tempContext.resolve(dockerfilePath.getFileName().toString());
          contextDir = tempContext;
        }
      } else {
        throw new IllegalStateException("No Dockerfile configured for image " + imageName);
      }

      buildImage(contextDir, dockerfile);
      resolvedImageName = imageName;
      return resolvedImageName;
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("Failed to build docker image " + imageName, e);
    } finally {
      if (tempContext != null) {
        try {
          FileUtils.deleteDirectory(tempContext.toFile());
        } catch (IOException e) {
          logger.warn("Could not delete temporary build context {}", tempContext, e);
        }
      }
    }
  }

  private void buildImage(Path contextDir, Path dockerfile)
      throws IOException, InterruptedException {
    var requestBuilder = DockerfileBuildRequest.builder(contextDir, dockerfile, imageName)
        .outputMode(BuildOutputMode.DOCKER);
    buildArgs.forEach(requestBuilder::buildArg);

    try (var client = new BuildkitClient(BUILDKIT_CONFIG)) {
      var result = client.buildImage(requestBuilder.build(), new PrintingListener());
      loadIntoDocker(result);
    } catch (BuildkitException e) {
      throw new RuntimeException(e);
    }
  }

  private void stageSupplementalFiles(Path contextDir) throws IOException {
    for (var entry : filesFromPath.entrySet()) {
      copyPath(entry.getValue(), contextDir.resolve(entry.getKey()));
    }
    for (var entry : filesFromClasspath.entrySet()) {
      copyClasspathResource(entry.getValue(), contextDir.resolve(entry.getKey()));
    }
  }

  private static void copyPath(Path source, Path destination) throws IOException {
    if (Files.isDirectory(source)) {
      FileUtils.copyDirectory(source.toFile(), destination.toFile());
      return;
    }

    if (destination.getParent() != null) {
      Files.createDirectories(destination.getParent());
    }
    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
  }

  private static void copyClasspathResource(String resourcePath, Path destination)
      throws IOException {
    URL resource = DockerImage.class.getResource(resourcePath);
    if (resource == null) {
      throw new IllegalStateException("Could not find classpath resource " + resourcePath);
    }

    if ("file".equals(resource.getProtocol())) {
      try {
        copyPath(Path.of(resource.toURI()), destination);
        return;
      } catch (URISyntaxException e) {
        throw new IOException("Invalid classpath resource URI for " + resourcePath, e);
      }
    }

    if (destination.getParent() != null) {
      Files.createDirectories(destination.getParent());
    }
    try (InputStream inputStream = resource.openStream()) {
      Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static String normalizeContextPath(String path) {
    String normalized = path.replace('\\', '/');
    while (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }
    while (normalized.startsWith("./")) {
      normalized = normalized.substring(2);
    }
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("Context path must not be blank");
    }
    return normalized;
  }

  private static void loadIntoDocker(BuildResult result) throws IOException, InterruptedException {
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
      logger.info("Loading image was successful.");
    } finally {
      Files.deleteIfExists(exportedArchive);
    }
  }

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
}
