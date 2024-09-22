package vadl.test.iss;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassOrder;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.DockerExecutionTest;

public abstract class QemuIssTest extends DockerExecutionTest {

  private static final ConcurrentHashMap<String, ImageFromDockerfile> issImageCache =
      new ConcurrentHashMap<>();

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
        return getIssImage(issOutputPath, true);
      } catch (IOException | DuplicatedPassKeyException e) {
        throw new RuntimeException(e);
      }
    });
  }


  private ImageFromDockerfile getIssImage(Path generatedIssSources,
                                          boolean precompile
  ) {

    return new ImageFromDockerfile()
        .withDockerfileFromBuilder(d -> {
              d
                  .from("jozott/qemu")
                  .copy("iss", "/qemu");
              if (precompile) {
                d.workDir("/qemu/build");
                // TODO: update target name
                d.run("../configure --target-list=vadl-softmmu");
                d.run("make -j 8");
                // validate existence of vadl
                d.run("ls qemu-system-vadl");

                d.workDir("/work");
              }
              d.build();
            }
        )
        .withFileFromPath("iss", generatedIssSources);
  }


}
