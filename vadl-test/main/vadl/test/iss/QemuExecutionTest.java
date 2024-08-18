package vadl.test.iss;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.test.DockerExecutionTest;
import vadl.test.FileUtils;
import vadl.viam.Identifier;
import vadl.viam.Specification;

/**
 * A ut
 */
abstract public class QemuExecutionTest extends DockerExecutionTest {

  // a cache that contains for the docker image with a newly created qemu build
  // for a Specification stored as its Identifier
  private static Map<Identifier, ImageFromDockerfile> specQemuBuildImageCache = new HashMap<>();

  public static synchronized ImageFromDockerfile getQemuTestImage(Path qemuSourceDir,
                                                                  Specification spec) {
    if (specQemuBuildImageCache.containsKey(spec.identifier)) {
      return specQemuBuildImageCache.get(spec.identifier);
    }


    var image = new ImageFromDockerfile()
        .withDockerfileFromBuilder(builder ->
            builder
                .from("jozott/qemu-rv64")
                // TODO copy emitted qemu files into /qemu
                .run("cd /qemu/build && make")
                .copy("/scripts/iss_qemu", "/scripts")
                .cmd(". /scripts/.venv/bin/activate && python3 /scripts/bare_metal_runner.py")
                .build()
        )
        .withFileFromClasspath("/scripts/iss_qemu", "/scripts/iss_qemu");

    specQemuBuildImageCache.put(spec.identifier, image);
    return image;
  }


}
