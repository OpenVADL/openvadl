package vadl.test.iss;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassOrder;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.DockerExecutionTest;

public abstract class QemuIssTest extends DockerExecutionTest {

  private static final String REDIS_CACHE_HOST = "ea.complang.tuwien.ac.at";
  private static final int REDIS_CACHE_PORT = 6379;

  private static final ConcurrentHashMap<String, ImageFromDockerfile> issImageCache =
      new ConcurrentHashMap<>();
  private static final Logger log = LoggerFactory.getLogger(QemuIssTest.class);

  protected ImageFromDockerfile generateSimulator(String specPath) {
    return issImageCache.computeIfAbsent(specPath, (path) -> {
      try {
        var config = IssConfiguration.from(getConfiguration(false));
        // run iss generation
        setupPassManagerAndRunSpec(path, PassOrder.iss(config));

        // find iss output path
        var issOutputPath = Path.of(config.outputPath() + "/iss").toAbsolutePath();
        if (!issOutputPath.toFile().exists()) {
          throw new IllegalStateException("ISS output path was not found (not generated?)");
        }

        // generate iss image from the output path
        return getIssImage(issOutputPath);
      } catch (IOException | DuplicatedPassKeyException e) {
        throw new RuntimeException(e);
      }
    });
  }


  private ImageFromDockerfile getIssImage(Path generatedIssSources
  ) {

    return new ImageFromDockerfile()
        .withDockerfileFromBuilder(d -> {
              d
                  .from(
                      "jozott/qemu@sha256:59fd89489908864d9c5267a39152a55559903040299bcb7a65382c4beebac2e2")
                  .copy("iss", "/qemu")
                  .workDir("/qemu/build");

              var cc = "gcc";
              if (testRedisCacheConnection()) {
                log.info("Redis cache connection established. Using sccache cache...");
                // TODO: Set redis port to our cache
                d.env("SCCACHE_REDIS_ENDPOINT", "tcp://" + REDIS_CACHE_HOST + ":" + REDIS_CACHE_PORT);
                cc = "sccache gcc";
              } else {
                log.warn("Couldn't connect to redis sccache. Building without cache...");
              }

              d.run("../configure --cc='" + cc + "' --target-list=vadl-softmmu");
              // TODO: update target name
              d.run("make -j 8");
              // validate existence of vadl
              d.run("qemu-system-vadl --help");

              d.workDir("/work");

              d.build();
            }
        )
        .withFileFromPath("iss", generatedIssSources);
  }

  private boolean testRedisCacheConnection() {
    var timeout = 2000;
    try (Socket socket = new Socket()) {
      socket.connect(
          new java.net.InetSocketAddress(REDIS_CACHE_HOST, REDIS_CACHE_PORT)
          , timeout);
      return true; // The port is open
    } catch (IOException e) {
      return false; // The port is not open, or the host is not reachable
    }
  }


}
