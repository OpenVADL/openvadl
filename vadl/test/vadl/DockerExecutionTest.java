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

package vadl;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static vadl.DockerBuildKitDaemon.BUILDKIT_DAEMON_PORT;

import com.github.dockerjava.api.DockerClient;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.utility.ThrowingFunction;
import vadl.utils.Pair;

public abstract class DockerExecutionTest extends AbstractTest {

  private static final Logger logger = LoggerFactory.getLogger(DockerExecutionTest.class);

  @LazyInit
  private static Network testNetwork;
  private static DockerBuildKitDaemon buildkitDaemon;

  @BeforeAll
  public static void beforeAll() {
    testNetwork = Network.newNetwork();
    logger.info("Created test network with id {}", testNetwork.getId());
    buildkitDaemon = DockerBuildKitDaemon.getRunningInstance();
  }

  @AfterAll
  public static void afterAll() {
    testNetwork.close();
  }

  /**
   * Read the file from {@code resultPath} line by line and assert that the status is zero.
   */
  protected List<DynamicTest> assertStatusCodes(String resultPath)
      throws IOException {
    ArrayList<DynamicTest> tests = new ArrayList<>();
    try (Stream<String> stream = Files.lines(Paths.get(resultPath))) {
      stream.forEach(x -> {
        var split = x.split(",");
        var name = split[0];
        var statusCode = split[1];

        tests.add(DynamicTest.dynamicTest(name,
            () -> assertEquals("0", statusCode)));
      });
    }

    return tests;
  }

  /**
   * Starts a container and checks the status code for the exited container.
   * It will copy the copy mappings into the container. After the container was
   * executed it will copy a file back to read the result.
   * It will assert that the status code is zero. If the check takes longer
   * than 10 seconds or the status code is not zero then it will throw an
   * exception.
   *
   * @param image               is the docker image for the {@link GenericContainer}.
   * @param copyMappings        are mappings from the host to the container for the files which should
   *                            be copied.
   * @param hostOutputPath      is the path where the {@code containerResultPath} should be copied
   *                            to.
   * @param containerResultPath is the path of a file which the container has computed and should
   *                            be copied to the host.
   */
  protected void runContainerAndCopyDirectoryIntoContainerAndCopyOutputBack(
      DockerImage image,
      List<Pair<String, String>> copyMappings,
      String hostOutputPath,
      String containerResultPath) {
    runContainerAndCopyDirectoryIntoContainerAndCopyOutputBack(image, copyMappings, hostOutputPath,
        containerResultPath, null);
  }


  /**
   * Starts a container and checks the status code for the exited container.
   * It will copy the copy mappings into the container. After the container was
   * executed it will copy a file back to read the result.
   * It will assert that the status code is zero. If the check takes longer
   * than 10 seconds or the status code is not zero then it will throw an
   * exception.
   *
   * @param image               is the docker image for the {@link GenericContainer}.
   * @param copyMappings        are mappings from the host to the container for the files which should
   *                            be copied.
   * @param hostOutputPath      is the path where the {@code containerResultPath} should be copied
   *                            to.
   * @param containerResultPath is the path of a file which the container has computed and should
   *                            be copied to the host.
   * @param cmd                 overwrites the command which will be executed on startup.
   */
  protected void runContainerAndCopyDirectoryIntoContainerAndCopyOutputBack(
      String image,
      List<Pair<String, String>> copyMappings,
      String hostOutputPath,
      String containerResultPath,
      @Nullable String cmd) {
    runContainer(image, (container) -> {
          if (cmd != null) {
            container.setCommand(cmd);
          }
          for (var mapping : copyMappings) {
            container.withCopyToContainer(MountableFile.forHostPath(mapping.left()), mapping.right());
          }
          return container;
        },
        (container) -> container.copyFileFromContainer(containerResultPath, hostOutputPath)
    );
  }

  /**
   * Starts a container and checks the status code for the exited container.
   * It will copy the copy mappings into the container. After the container was
   * executed it will copy a file back to read the result.
   * It will assert that the status code is zero. If the check takes longer
   * than 10 seconds or the status code is not zero then it will throw an
   * exception.
   *
   * @param image               is the docker image for the {@link GenericContainer}.
   * @param copyMappings        are mappings from the host to the container for the files which should
   *                            be copied.
   * @param hostOutputPath      is the path where the {@code containerResultPath} should be copied
   *                            to.
   * @param containerResultPath is the path of a file which the container has computed and should
   *                            be copied to the host.
   * @param cmd                 overwrites the command which will be executed on startup.
   */
  protected void runContainerAndCopyDirectoryIntoContainerAndCopyOutputBack(
      DockerImage image,
      List<Pair<String, String>> copyMappings,
      String hostOutputPath,
      String containerResultPath,
      @Nullable String cmd) {
    int mappedPort = buildkitDaemon.getMappedPort(BUILDKIT_DAEMON_PORT);
    runContainerAndCopyDirectoryIntoContainerAndCopyOutputBack(
        image.getDockerImageName(mappedPort),
        copyMappings, hostOutputPath, containerResultPath, cmd);
  }

  /**
   * Starts a container and checks the status code for the exited container.
   * It will copy the copy mappings into the container. After the container was
   * executed it will copy the specified mappings back.
   * It will assert that the status code is zero. If the check takes longer
   * than 10 seconds or the status code is not zero then it will throw an
   * exception.
   *
   * @param image          is the docker image for the {@link GenericContainer}.
   * @param inputMappings  are mappings from the host to the container for the files which
   *                       should be copied.
   * @param outputMappings are mappings from the container to the host for the files which
   *                       should be copied back after running the container.
   * @param cmd            overwrites the command which will be executed on startup.
   */
  protected void runContainerWithInAndOutput(
      ImageFromDockerfile image,
      List<Pair<String, String>> inputMappings,
      List<Pair<String, String>> outputMappings,
      @Nullable String cmd) {
    runContainer(image, (container) -> {
          if (cmd != null) {
            container.setCommand(cmd);
          }
          for (var mapping : inputMappings) {
            container.withCopyToContainer(MountableFile.forHostPath(mapping.left()), mapping.right());
          }
          return container;
        },
        (container) -> {
          for (var mapping : outputMappings) {
            try {
              Files.createDirectories(new File(mapping.right()).getParentFile().toPath());
            } catch (IOException e) {
              Assertions.fail(e);
            }
            container.copyFileFromContainer(mapping.left(), mapping.right());
          }
        }
    );
  }

  /**
   * Starts a container and checks the status code for the exited container.
   * It will write the given {@code content} into a temporary file. The
   * temporary file requires a {@code prefix} and {@code suffix}.
   * It will assert that the status code is zero. If the check takes longer
   * than 10 seconds or the status code is not zero then it will throw an
   * exception.
   *
   * @param image         is the docker image for the {@link GenericContainer}.
   * @param containerPath is the path where the {@code path} should be copied to.
   * @param content       is the content of file which will be written to the
   *                      temp file.
   * @throws IOException when the temp file is writable.
   */
  protected void runContainerAndCopyInputIntoContainer(
      DockerImage image,
      String content,
      String containerPath) throws IOException {
    runContainer(image, (container) -> container
            .withCopyToContainer(Transferable.of(content), containerPath),
        null
    );
  }

  /**
   * Starts a container and checks the status code for the exited container.
   * It will write the given {@code content} into a temporary file. The
   * temporary file requires a {@code prefix} and {@code suffix}.
   * Copies the data from {@code copyMappings}. Additionally, it will
   * set environment variables based on {@code environmentMappings}.
   *
   * @param image               is the docker image for the {@link GenericContainer}.
   * @param copyMappings        is a list where each {@link Pair} indicates what should be copied
   *                            from the host to the container.
   * @param environmentMappings is a list where each entry defines an environment variable which
   *                            will be set in the container.
   * @param cmd                 is the command which is executed.
   */
  protected void runContainerAndCopyInputIntoContainer(
      DockerImage image,
      List<Pair<Path, String>> copyMappings,
      Map<String, String> environmentMappings,
      String cmd) {
    var mappedPort = buildkitDaemon.getMappedPort(BUILDKIT_DAEMON_PORT);
    runContainerAndCopyInputIntoContainerAndCopyFromContainerToHost(
        image.getDockerImageName(mappedPort),
        copyMappings,
        environmentMappings,
        Collections.emptyList(),
        cmd);
  }


  /**
   * Starts a container and checks the status code for the exited container.
   * It will write the given {@code content} into a temporary file. The
   * temporary file requires a {@code prefix} and {@code suffix}.
   * Copies the data from {@code copyMappings}. Additionally, it will
   * set environment variables based on {@code environmentMappings}.
   *
   * @param image               is the docker image for the {@link GenericContainer}.
   * @param copyMappings        is a list where each {@link Pair} indicates what should be copied
   *                            from the host to the container.
   * @param environmentMappings is a list where each entry defines an environment variable which
   *                            will be set in the container.
   * @param cmd                 is the command which is executed.
   */
  protected void runContainerAndCopyInputIntoContainer(
      String image,
      List<Pair<Path, String>> copyMappings,
      Map<String, String> environmentMappings,
      String cmd) {
    runContainerAndCopyInputIntoContainerAndCopyFromContainerToHost(image,
        copyMappings,
        environmentMappings,
        Collections.emptyList(),
        cmd);
  }

  /**
   * Starts a container and checks the status code for the exited container.
   * It will write the given {@code content} into a temporary file. The
   * temporary file requires a {@code prefix} and {@code suffix}.
   * Copies the data from {@code copyMappings}. Additionally, it will
   * set environment variables based on {@code environmentMappings}. The {@code copyFromContainerToHost}
   * can be used to define mappings between host and guest system.
   *
   * @param image                   is the docker image for the {@link GenericContainer}.
   * @param copyMappings            is a list where each {@link Pair} indicates what should be copied
   *                                from the host to the container.
   * @param environmentMappings     is a list where each entry defines an environment variable which
   *                                will be set in the container.
   * @param copyFromContainerToHost copies from guest to host.
   * @param cmd                     is the command which is executed.
   */
  protected void runContainerAndCopyInputIntoContainerAndCopyFromContainerToHost(
      String image,
      List<Pair<Path, String>> copyMappings,
      Map<String, String> environmentMappings,
      List<Pair<String, String>> copyFromContainerToHost,
      String cmd) {
    runContainer(image, (container) -> {
      for (var mapping : copyMappings) {
        container
            .withCopyFileToContainer(
                MountableFile.forHostPath(mapping.left()),
                mapping.right());
      }

      for (var mapping : environmentMappings.entrySet()) {
        container
            .withEnv(
                mapping.getKey(),
                mapping.getValue());
      }

      container.withCommand(cmd);

      return container;
    }, (container) -> {
      for (var mapping : copyFromContainerToHost) {
        container.copyFileFromContainer(mapping.left(), mapping.right());
      }
    });
  }

  /**
   * Starts a container and checks the status code for the exited container.
   * It will assert that the status code is zero. If the check takes longer
   * than 10 seconds or the status code is not zero then it will throw an
   * exception.
   *
   * @param image             is the docker image for the {@link GenericContainer}.
   * @param containerModifier a consumer that allows modification of the container configuration
   * @param postExecution     a consumer that is called when the container successfully terminated
   */
  protected void runContainer(String image,
                              Function<GenericContainer<?>, GenericContainer<?>> containerModifier,
                              @Nullable Consumer<GenericContainer<?>> postExecution
  ) {
    try (GenericContainer<?> container = new GenericContainer<>(image)
        .withLogConsumer(new Slf4jLogConsumer(logger))
        .withNetwork(testNetwork)
        .withStartupAttempts(1)) {
      var modifiedContainer = containerModifier.apply(container);
      modifiedContainer.setStartupAttempts(1);
      modifiedContainer.start();

      await()
          .atMost(Duration.ofSeconds(2000))
          .until(() -> {
            var result =
                modifiedContainer.getDockerClient()
                    .inspectContainerCmd(modifiedContainer.getContainerId());
            var state = result.exec().getState();
            return state.getStatus().equals("exited");
          });

      var result = modifiedContainer.getDockerClient()
          .inspectContainerCmd(modifiedContainer.getContainerId());

      var state = result.exec().getState();
      assertEquals(0, state.getExitCodeLong().intValue());

      if (postExecution != null) {
        postExecution.accept(modifiedContainer);
      }
    }
  }

  /**
   * Starts a container and checks the status code for the exited container.
   * It will assert that the status code is zero. If the check takes longer
   * than 10 seconds or the status code is not zero then it will throw an
   * exception.
   *
   * @param image             is the docker image for the {@link GenericContainer}.
   * @param containerModifier a consumer that allows modification of the container configuration
   * @param postExecution     a consumer that is called when the container successfully terminated
   */
  protected void runContainer(DockerImage image,
                              Function<GenericContainer<?>, GenericContainer<?>> containerModifier,
                              @Nullable Consumer<GenericContainer<?>> postExecution
  ) {
    var mapedPort = buildkitDaemon.getMappedPort(BUILDKIT_DAEMON_PORT);
    runContainer(image.getDockerImageName(mapedPort), containerModifier, postExecution);
  }

  public static Network testNetwork() {
    return testNetwork;
  }

  /**
   * Copies a path from a container to the host system.
   *
   * @param container     the {@link GenericContainer} from which to copy the path
   * @param containerPath the path inside the container that should be copied
   * @param hostPath      the path on the host system where the content should be copied to
   * @return the container from which the path was copied for potential chaining of other operations
   */
  public static GenericContainer<?> copyPathFromContainer(GenericContainer<?> container,
                                                          String containerPath,
                                                          Path hostPath) {
    copyPathFromContainer(container, containerPath, (tarStream) -> {
      var currentEntry = tarStream.getCurrentEntry();

      // copy is file only (no directory copy)
      var fileOnly = currentEntry.isFile();
      // in case of coping a directory,
      // we don't want to emit the root directory (would result in double nested directories)
      var dirPrefixToRemove = currentEntry.isDirectory() ? currentEntry.getName() : "";

      while (currentEntry != null) {
        File destFile;
        if (fileOnly) {
          // if we copy only a single file, we use the specified hostPath as destiniation
          destFile = hostPath.toFile();
        } else {
          // if we copy a directory we have to resolve the path
          destFile = hostPath.resolve(currentEntry.getName()
              // remove the root directory of the copied TAR
              .replaceFirst("^" + dirPrefixToRemove, "")
          ).toFile();
        }

        if (currentEntry.isFile()) {
          // create parent directory if they do not exist yet
          FileUtils.forceMkdirParent(destFile);
          // copy file to destination
          try (FileOutputStream output = new FileOutputStream(destFile)) {
            IOUtils.copy(tarStream, output);
          }
        } else if (currentEntry.isDirectory()) {
          if (destFile.exists() && !destFile.isDirectory()) {
            // throw exception if directory would override already existing file
            throw new IllegalStateException(
                "copyPathFromContainer cannot create directory %s as a file at this path already exists.".formatted(
                    destFile));
          }
          // create a destination directory
          FileUtils.forceMkdir(destFile);
        } else {
          // if we cannot handle the entry, we throw an exception
          throw new IllegalStateException(
              "copyPathFromContainer can only copy files and directories. %s is neither a file nor a directory."
                  .formatted(currentEntry.getName()));
        }

        // jump to next tar entry
        currentEntry = tarStream.getNextTarEntry();
      }

      return true;
    });

    return container;
  }

  /**
   * Streams a path as {@link TarArchiveInputStream} which resides in the container.
   * The stream is already advanced to the first entry.
   * So you want to call {@link TarArchiveInputStream#getCurrentEntry()} to read the first
   * tar entry.
   *
   * @param container     container to copy from
   * @param containerPath path inside container that should be copied
   * @param function      function that takes {@link TarArchiveInputStream} of the copied path
   * @return whatever the {@code function} parameter returns
   */
  public static <T> T copyPathFromContainer(GenericContainer<?> container, String containerPath,
                                            ThrowingFunction<TarArchiveInputStream, T> function) {
    if (container.getContainerId() == null) {
      throw new IllegalStateException(
          "copyFileFromContainer can only be used when the Container is created.");
    }

    DockerClient dockerClient = container.getDockerClient();
    try (
        InputStream inputStream = dockerClient.copyArchiveFromContainerCmd(
            container.getContainerId(), containerPath).exec();
        TarArchiveInputStream tarInputStream = new TarArchiveInputStream(inputStream)
    ) {
      tarInputStream.getNextTarEntry();
      return function.apply(tarInputStream);
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException(e);
    }
  }

  private static boolean startBuildkitDaemon() {
    var client = DockerClientFactory.instance().client();

    if (client == null) {
      throw new IllegalStateException("Docker client is not available");
    }

    var containers = client.listContainersCmd().exec();
    for (var container : containers) {

    }

    return true;
  }
}
