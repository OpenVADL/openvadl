package vadl.test;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.MountType;
import com.github.dockerjava.api.model.Volume;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.images.builder.dockerfile.DockerfileBuilder;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.utility.ThrowingFunction;

public abstract class DockerExecutionTest extends AbstractTest {

  private static final Logger logger = LoggerFactory.getLogger(DockerExecutionTest.class);

  private static final Network testNetwork = Network.newNetwork();
  private static RedisCache redisCache = getRunningRedisCache();

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
  protected void runContainerAndCopyInputIntoContainer(ImageFromDockerfile image,
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
   * It will assert that the status code is zero. If the check takes longer
   * than 10 seconds or the status code is not zero then it will throw an
   * exception.
   * Copies the data from {@code content} to {@code mountPath} and copies
   * an archive from {@code containerMountPath + archiveName} to {@code hostPath + archiveName}.
   * Both {@code containerMountPath} and {@code hostPath} need to be paths.
   * This method will also automatically untar the file.
   *
   * @param image            is the docker image for the {@link GenericContainer}.
   * @param inContainerPath  is the path where the {@code path} should be mounted to.
   * @param inHostPath       is the content of file which will be written to the
   *                         temp file.
   * @param outHostPath      is the path on the host for the output archive.
   * @param outContainerPath is the path in the container for the output archive.
   */
  protected void runContainerAndCopyInputIntoAndCopyOutputFromContainer(ImageFromDockerfile image,
                                                                        Path inHostPath,
                                                                        String inContainerPath,
                                                                        Path outHostPath,
                                                                        String outContainerPath) {
    runContainer(image, (container) -> container
            .withCopyFileToContainer(MountableFile.forHostPath(inHostPath), inContainerPath),
        (container) -> copyPathFromContainer(container, outContainerPath, outHostPath)
    );
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
  protected void runContainer(ImageFromDockerfile image,
                              Function<GenericContainer<?>, GenericContainer<?>> containerModifier,
                              @Nullable Consumer<GenericContainer<?>> postExecution
  ) {
    try (GenericContainer<?> container = new GenericContainer<>(image)
        .withLogConsumer(new Slf4jLogConsumer(logger))
        .withNetwork(testNetwork)) {
      var modifiedContainer = containerModifier.apply(container);
      modifiedContainer.setStartupAttempts(1);
      modifiedContainer.start();

      await()
          .atMost(Duration.ofSeconds(20))
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
   * Returns a running redis cache. If no redis cache exists yet, it will be created.
   *
   * @return an object containing redis cache information
   */
  protected static synchronized RedisCache getRunningRedisCache() {
    if (redisCache != null && redisCache.redisContainer.isRunning()) {
      return redisCache;
    }

    var hostName = "redis";

    var container = new GenericContainer<>("redis:7.4")
        .withCreateContainerCmdModifier(cmd -> {
          var mount = new Mount()
              .withType(MountType.VOLUME)
              .withSource("open-vadl-redis-cache")
              .withTarget("/data");

          Objects.requireNonNull(cmd.getHostConfig())
              .withMounts(List.of(mount));
          cmd.withName("open-vadl-test-cache");
          cmd.withAliases("redis");
        })
        // we need this custom network, because other containers must access
        // the redis cache with the given hostname/alias
        // (which is only available on custom networks)
        .withNetwork(testNetwork)
        .withNetworkAliases(hostName);

    container.start();
    redisCache = new RedisCache(hostName, 6379, container);
    return redisCache;
  }

  public static Network testNetwork() {
    return testNetwork;
  }

  protected record RedisCache(
      String host,
      int port,
      GenericContainer<?> redisContainer
  ) {

    public void stop() {
      try {
        redisContainer.execInContainer("redis-cli", "shutdown", "save");
        redisContainer.stop();
      } catch (IOException | InterruptedException e) {
        redisContainer.stop();
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * This class abstracts the configuration for the redis cache.
   */
  protected static class SetupRedisEnv {

    private static final String SCCACHE_REDIS_ENDPOINT = "SCCACHE_REDIS_ENDPOINT";

    private static String tcpAddress() {
      return "tcp://" + Objects.requireNonNull(redisCache).host() + ":" + redisCache.port();
    }

    /**
     * Sets an environment variable to indicate to the distributed cache to use the redis
     * docker instance as cache.
     */
    public static void setupEnv(DockerfileBuilder d) {
      logger.info("Using redis cache: {}", redisCache);
      d.env(SCCACHE_REDIS_ENDPOINT, tcpAddress());

      // check if redis cache is available
      d.run(
          "timeout 5 bash -c '</dev/tcp/" + redisCache.host() + "/" + redisCache.port() + "'");
    }

    public static ImageFromDockerfile setupEnv(ImageFromDockerfile image) {
      image.withBuildArg(SCCACHE_REDIS_ENDPOINT, tcpAddress());

      return image;
    }
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


}
