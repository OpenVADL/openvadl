package vadl.test;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.MountType;
import com.github.dockerjava.api.model.Volume;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

public abstract class DockerExecutionTest extends AbstractTest {

  private static final Logger logger = LoggerFactory.getLogger(DockerExecutionTest.class);

  @Nullable
  private static RedisCache redisCache;

  @AfterAll
  public static synchronized void tearDown() {
    if (redisCache != null) {
      if (!redisCache.redisContainer.isRunning()) {
        redisCache.stop();
      }
      redisCache = null;
    }
  }

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
   * @param content   is the content of file which will be written to the
   *                  temp file.
   * @throws IOException when the temp file is writable.
   */
  protected void runContainerWithContent(ImageFromDockerfile image,
                                         String content,
                                         String mountPath) throws IOException {
    runContainer(image, (container) -> container
            .withCopyToContainer(Transferable.of(content), mountPath),
        null
    );
  }

  protected void runContainerWithHostFsBind(ImageFromDockerfile image,
                                            Path hostPath,
                                            String containerPath) {
    runContainer(image, container ->
            withHostFsBind(container, hostPath, containerPath),
        null
    );
  }

  protected void runContainer(ImageFromDockerfile image, Path hostPath, String mountPath) {
    runContainer(image, hostPath.toString(), mountPath);
  }

  /**
   * Starts a container and checks the status code for the exited container.
   * It will assert that the status code is zero. If the check takes longer
   * than 10 seconds or the status code is not zero then it will throw an
   * exception.
   *
   * @param image     is the docker image for the {@link GenericContainer}.
   * @param path      is the path of the file which will be mapped to the container.
   * @param mountPath is the path where the {@code path} should be mounted to.
   */
  protected void runContainer(ImageFromDockerfile image, String path, String mountPath) {
    runContainer(image, (container) -> container.withCopyToContainer(
            MountableFile.forHostPath(path), mountPath),
        null
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
                              Consumer<GenericContainer<?>> containerModifier,
                              @Nullable Consumer<GenericContainer<?>> postExecution
  ) {
    try (GenericContainer<?> container = new GenericContainer<>(image)
        .withLogConsumer(new Slf4jLogConsumer(logger))) {
      containerModifier.accept(container);
      container.start();

      await()
          .atMost(Duration.ofSeconds(20))
          .until(() -> {
            var result =
                container.getDockerClient().inspectContainerCmd(container.getContainerId());
            var state = result.exec().getState();
            return state.getStatus().equals("exited");
          });

      var result = container.getDockerClient().inspectContainerCmd(container.getContainerId());

      var state = result.exec().getState();
      assertEquals(0, state.getExitCodeLong().intValue());

      if (postExecution != null) {
        postExecution.accept(container);
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

    var container = new GenericContainer<>("redis:7.4")
        .withCreateContainerCmdModifier(cmd -> {
          var mount = new Mount()
              .withType(MountType.VOLUME)
              .withSource("open-vadl-redis-cache")
              .withTarget("/data");

          Objects.requireNonNull(cmd.getHostConfig())
              .withMounts(List.of(mount));
        })
        .withExposedPorts(6379);
    container.start();
    redisCache = new RedisCache(container);
    return redisCache;
  }


  protected static <T extends GenericContainer<?>> T withHostFsBind(T container, Path hostPath,
                                                                    String containerPath) {
    // withFileSystemBind got deprecated and this is somewhat a replacement
    // https://github.com/testcontainers/testcontainers-java/pull/7652
    // https://github.com/joyrex2001/kubedock/issues/89
    container.withCreateContainerCmdModifier(cmd ->
        Objects.requireNonNull(cmd.getHostConfig())
            .withBinds(Bind.parse(hostPath.toAbsolutePath() + ":" + containerPath))
    );
    return container;
  }

  protected record RedisCache(
      String host,
      int port,
      GenericContainer<?> redisContainer
  ) {

    RedisCache(GenericContainer<?> redisContainer) {
      this(redisContainer.getHost(), redisContainer.getMappedPort(6379), redisContainer);
    }

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
}
