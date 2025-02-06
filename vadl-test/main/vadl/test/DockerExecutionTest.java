package vadl.test;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.MountType;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.images.builder.dockerfile.DockerfileBuilder;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.utility.ThrowingFunction;
import vadl.utils.Pair;

public abstract class DockerExecutionTest extends AbstractTest {

  private static final Logger logger = LoggerFactory.getLogger(DockerExecutionTest.class);

  @LazyInit
  private static Network testNetwork;

  @Nullable
  private static RedisCache redisCache;

  @BeforeAll
  public static void beforeAll() {
    testNetwork = Network.newNetwork();
    logger.info("Created test network with id {}", testNetwork.getId());
  }

  @AfterAll
  public static void afterAll() {
    if (redisCache != null) {
      // this call will also persist the data cached in this test run
      redisCache.stop();
    }

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
            () -> Assertions.assertEquals("0", statusCode)));
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
      ImageFromDockerfile image,
      List<Pair<String, String>> copyMappings,
      String hostOutputPath,
      String containerResultPath) {
    runContainer(image, (container) -> {
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
   * Copies the data from {@code copyMappings}. Additionally, it will
   * set environment variables based on {@code environmentMappings}.
   *
   * @param image               is the docker image for the {@link GenericContainer}.
   * @param copyMappings        is a list where each {@link Pair} indicates what should be copied
   *                            from the host to the container.
   * @param environmentMappings is a list where each entry defines an environment variable which
   *                            will be set in the container.
   */
  protected void runContainerAndCopyInputIntoContainer(ImageFromDockerfile image,
                                                       List<Pair<Path, String>> copyMappings,
                                                       Map<String, String> environmentMappings) {
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

      return container;
    }, (container) -> {
    });
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
  protected void runContainerAndCopyInputIntoContainer(ImageFromDockerfile image,
                                                       List<Pair<Path, String>> copyMappings,
                                                       Map<String, String> environmentMappings,
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
  protected void runContainer(ImageFromDockerfile image,
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

  public static Network testNetwork() {
    return testNetwork;
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

    redisCache = new RedisCache("redis", testNetwork());
    redisCache.start();
    return redisCache;
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


  /**
   * A containerized redis cache that persists the cached data across runs.
   * If your test container should use the cache (e.g. via sccache) you must run the container
   * (or build step) in the same network as the redis cache.
   * The easiest way to do is by using the {@link RedisCache#setupEnv(ImageFromDockerfile)}
   * and {@link RedisCache#setupEnv(DockerfileBuilder)}.
   * While you must use the first method, the second one is only useful if you use
   * the {@link DockerfileBuilder}.
   */
  public record RedisCache(
      String host,
      int port,
      GenericContainer<?> redisContainer,
      Network network
  ) {

    private static final Logger log = LoggerFactory.getLogger(RedisCache.class);

    private static final String SCCACHE_REDIS_ENDPOINT = "SCCACHE_REDIS_ENDPOINT";

    RedisCache(String hostName, Network network) {
      this(hostName, 6379, constructContainer(hostName, network), network);
    }


    private void start() {
      redisContainer.start();
    }

    /**
     * This will stop the redis cache if it is still running.
     * It will also persist the data cached during this run, so it is available in
     * the next run.
     */
    private void stop() {
      try {
        if (!redisContainer.isRunning()) {
          log.info("Redis container isn't running anymore. Skipping shutdown.");
          return;
        }
        // persist cache before shutting down
        log.info("Persist redis cache data before shutdown...");
        redisContainer.execInContainer("redis-cli", "shutdown", "save");

        redisContainer.stop();
        log.info("Stopped redis cache container.");
      } catch (IOException | InterruptedException e) {
        redisContainer.stop();
        throw new RuntimeException(e);
      }
    }

    /**
     * Sets an environment variable to indicate to the distributed cache to use the redis
     * docker instance as cache.
     */
    public DockerfileBuilder setupEnv(DockerfileBuilder d) {
      logger.info("Using redis cache: {}", redisCache);
      d.env(SCCACHE_REDIS_ENDPOINT, tcpAddress());

      // check if redis cache is available
      d.run(
          "timeout 5 bash -c '</dev/tcp/" + redisCache.host() + "/" + redisCache.port() + "'");
      return d;
    }

    /**
     * Sets the sccache redis endpoint argument and configures the used build network to be
     * the same as the one of the redis container.
     */
    public ImageFromDockerfile setupEnv(ImageFromDockerfile image) {
      image.withBuildArg(SCCACHE_REDIS_ENDPOINT, tcpAddress())
          .withBuildImageCmdModifier(modifier -> modifier.withNetworkMode(network.getId()));

      return image;
    }

    private String tcpAddress() {
      return "tcp://" + host + ":" + port;
    }

    /**
     * Constructs a redis container that is mount to a cache volume.
     * And exposed to the given network and the given hostName.
     */
    private static GenericContainer<?> constructContainer(String hostName,
                                                          Network network) {
      return new GenericContainer<>("redis:7.4")
          .withCreateContainerCmdModifier(cmd -> {
            var mount = new Mount()
                .withType(MountType.VOLUME)
                .withSource("open-vadl-redis-cache")
                .withTarget("/data");

            Objects.requireNonNull(cmd.getHostConfig())
                .withMounts(List.of(mount));
            cmd.withName("open-vadl-test-cache-" + network.getId());
          })
          // we need this custom network, because other containers must access
          // the redis cache with the given hostname/alias
          // (which is only available on custom networks)
          .withNetwork(network)
          .withNetworkAliases(hostName);
    }
  }

}
