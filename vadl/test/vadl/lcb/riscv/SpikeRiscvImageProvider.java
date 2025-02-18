package vadl.lcb.riscv;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.DockerExecutionTest;

/**
 * A singleton implementation to keep the reference to a {@link ImageFromDockerfile} to avoid
 * recompilation between {@link LlvmRiscvAssemblyTest} and {@link SpikeRiscvSimulationTest}.
 */
public class SpikeRiscvImageProvider {
  private static Map<String, ImageFromDockerfile> images = new HashMap<>();

  /**
   * Create an {@link ImageFromDockerfile} or return an already existing image.
   *
   * @param redisCache          is the cache for building LLVM.
   * @param pathDockerFile      is the path to the dockerfile which should be built.
   * @param target              is the name of the processor.
   * @param upstreamBuildTarget is the name of LLVM backend to compile an upstream compiler.
   * @param upstreamClangTarget is the name for the LLVM clang option to invoke the upstream
   *                            compiler.
   * @param spikeTarget         is the ISA for spike to run the executable.
   * @param doDebug             if the flag is {@code true} then the image will not be deleted.
   * @throws RuntimeException when the {@code isCI} environment variable and {@code doDebug} are
   *                          activated.
   */
  public static ImageFromDockerfile image(DockerExecutionTest.RedisCache redisCache,
                                          String pathDockerFile,
                                          String target,
                                          String upstreamBuildTarget,
                                          String upstreamClangTarget,
                                          String spikeTarget,
                                          boolean doDebug) {
    var image = images.get(target);
    if (image == null) {

      var deleteOnExit = !doDebug;

      if ("true".equals(System.getenv("isCI")) && !deleteOnExit) {
        throw new RuntimeException("It is not allowed to activate 'deleteOnExit' in the CI");
      }

      var img = redisCache.setupEnv(new ImageFromDockerfile("tc_spike_riscv"
          + target, deleteOnExit)
          .withDockerfile(Paths.get(pathDockerFile))
          .withBuildArg("TARGET", target)
          .withBuildArg("UPSTREAM_BUILD_TARGET", upstreamBuildTarget)
          .withBuildArg("UPSTREAM_CLANG_TARGET", upstreamClangTarget)
          .withBuildArg("SPIKE_TARGET", spikeTarget));
      images.put(target, img);
      return img;
    } else {
      return image;
    }
  }
}
