package vadl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

public abstract class DockerExecutionTest extends AbstractTest {

  private static final Logger logger = LoggerFactory.getLogger(DockerExecutionTest.class);

  private File writeCodeIntoTempFile(String content, String prefix, String suffix)
      throws IOException {
    var tempFile = File.createTempFile(prefix, suffix);
    tempFile.deleteOnExit();
    var writer = new FileWriter(tempFile);
    writer.write(content);
    writer.close();
    return tempFile;
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
   * @param prefix    is required for the temp file.
   * @param suffix    is required for the temp file.
   * @throws IOException when the temp file is writable.
   */
  protected void runContainerWithContent(ImageFromDockerfile image,
                                         String content,
                                         String mountPath,
                                         String prefix,
                                         String suffix) throws IOException {
    var file = writeCodeIntoTempFile(content, prefix, suffix);
    runContainerWithFile(image, file.getPath(), mountPath);
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
  protected void runContainerWithFile(ImageFromDockerfile image, String path, String mountPath) {
    try (GenericContainer<?> container = new GenericContainer<>(image).withCopyFileToContainer(
            MountableFile.forHostPath(path), mountPath)
        .withLogConsumer(new Slf4jLogConsumer(logger))) {
      container.start();

      await()
          .atMost(Duration.ofSeconds(10))
          .until(() -> {
            var result =
                container.getDockerClient().inspectContainerCmd(container.getContainerId());
            var state = result.exec().getState();
            return state.getStatus().equals("exited");
          });

      var result = container.getDockerClient().inspectContainerCmd(container.getContainerId());
      var state = result.exec().getState();
      assertThat(state.getExitCodeLong()).isZero();
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
        .withStartupAttempts(1)) {
      var modifiedContainer = containerModifier.apply(container);
      modifiedContainer.setStartupAttempts(1);
      modifiedContainer.start();

      await()
          .atMost(Duration.ofSeconds(30))
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
}
