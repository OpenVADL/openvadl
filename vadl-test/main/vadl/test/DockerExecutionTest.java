package vadl.test;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.MountType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.images.builder.dockerfile.DockerfileBuilder;
import org.testcontainers.utility.MountableFile;

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
   * @param image     is the docker image for the {@link GenericContainer}.
   * @param mountPath is the path where the {@code path} should be mounted to.
   * @param path   is the content of file which will be written to the
   *                  temp file.
   * @throws IOException when the temp file is writable.
   */
  protected void runContainerAndCopyInputIntoContainer(ImageFromDockerfile image,
                                                       String path,
                                                       String mountPath) throws IOException {
    runContainer(image, (container) -> container
            .withCopyToContainer(Transferable.of(path), mountPath),
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
   * @param image              is the docker image for the {@link GenericContainer}.
   * @param mountPath          is the path where the {@code path} should be mounted to.
   * @param content            is the content of file which will be written to the
   *                           temp file.
   * @param hostPath           is the path on the host for the output archive.
   * @param containerMountPath is the path in the container for the output archive.
   * @param archiveName        is the name of the archive in {@code hostPath} and {@code archiveName}.
   */
  protected void runContainerAndCopyInputIntoAndCopyOutputFromContainer(ImageFromDockerfile image,
                                                                        String content,
                                                                        String mountPath,
                                                                        String hostPath,
                                                                        String containerMountPath,
                                                                        String archiveName) {
    runContainer(image, (container) -> container
            .withCopyFileToContainer(MountableFile.forHostPath(Path.of(content)), mountPath),
        (container) -> {
          container.copyFileFromContainer(containerMountPath + "/" + archiveName,
              hostPath + "/" + archiveName);
          try {
            untar(new File(hostPath + "/" + archiveName), new File(hostPath));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
    );
  }

  private static void untar(File tarFile, File outputDir) throws IOException {
    try (FileInputStream fis = new FileInputStream(tarFile);
         TarArchiveInputStream tais = new TarArchiveInputStream(fis)) {

      TarArchiveEntry entry;
      while ((entry = tais.getNextTarEntry()) != null) {
        File outputFile = new File(outputDir, entry.getName());

        // If entry is a directory, create it
        if (entry.isDirectory()) {
          outputFile.mkdirs();
        } else {
          // Ensure parent directory exists
          outputFile.getParentFile().mkdirs();
          try (OutputStream os = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = tais.read(buffer)) != -1) {
              os.write(buffer, 0, len);
            }
          }
        }
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
}
