package vadl.test.iss;

import static java.lang.Thread.sleep;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class DummyTest extends QemuExecutionTest {

  @Test
  void test() throws InterruptedException, IOException {
    var spec = runAndGetViamSpecification("examples/rv3264im.vadl");
    var image = getQemuTestImage(Path.of("./"), spec);

    runContainerWithContent(
        image,
        """
            .global _start
            .section .text.bios
                    
            _start:
             li a0, 0xABCDEF9876543210
             # li a0, 0x68656c6c6f7c0a0a
             li a1, 0x10000000
             li a2, 0xFFFFFF0001000FFF
                    
            # the qmp script polls this t1 to check whether the test has ended
            signal_stop:
              li t1, 0xde
                    
            loop:	j loop
                    
            """,
        "/work/hello64",
        "testfile",
        "nonworking"
    );
  }

  void runTest(ImageFromDockerfile image) {
    try (GenericContainer<?> container = new GenericContainer<>(image)
        .withCommand("ls")) {
      container.start();

      await()
          .atMost(Duration.ofSeconds(20));

      var logs = container.getLogs();
      System.out.println("-------- logs: " + logs);
    }
  }

}
