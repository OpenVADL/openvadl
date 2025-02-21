package vadl.lcb.riscv;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.DockerExecutionTest;

/**
 * A singleton implementation to keep the reference to a {@link ImageFromDockerfile} to avoid
 * recompilation between {@link EmbenchSpikeRiscv32SimulationTest}.
 */
public class EmbenchImageProvider {
  private static Map<String, ImageFromDockerfile> images = new HashMap<>();

  /**
   * Create an {@link ImageFromDockerfile} or return an already existing image.
   *
   * @param redisCache          is the cache for building LLVM.
   * @param pathDockerFile      is the path to the dockerfile which should be built.
   * @param target              is the name of the processor.
   * @param upstreamBuildTarget is the name of LLVM backend to compile an upstream compiler.
   * @param doDebug             if the flag is {@code true} then the image will not be deleted.
   * @throws RuntimeException when the {@code isCI} environment variable and {@code doDebug} are
   *                          activated.
   */
  public static ImageFromDockerfile image(DockerExecutionTest.RedisCache redisCache,
                                          String pathDockerFile,
                                          String target,
                                          String upstreamBuildTarget,
                                          boolean doDebug) {
    var image = images.get(target);
    if (image == null) {

      var deleteOnExit = !doDebug;

      if ("true".equals(System.getenv("isCI")) && !deleteOnExit) {
        throw new RuntimeException("It is not allowed to activate 'deleteOnExit' in the CI");
      }

      var img = redisCache.setupEnv(new ImageFromDockerfile("tc_embench_"
          + target, deleteOnExit)
          .withDockerfile(Paths.get(pathDockerFile))
          .withBuildArg("TARGET", target)
          .withBuildArg("UPSTREAM_BUILD_TARGET", upstreamBuildTarget));
      images.put(target, img);
      return img;
    } else {
      return image;
    }
  }
}
