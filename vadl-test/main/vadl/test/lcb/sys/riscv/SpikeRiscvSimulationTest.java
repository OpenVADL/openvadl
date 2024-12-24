package vadl.test.lcb.sys.riscv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.lcb.AbstractLcbTest;

public abstract class SpikeRiscvSimulationTest extends AbstractLcbTest {
  protected abstract String getTarget();

  protected abstract String getUpstreamBuildTarget();

  protected abstract String getUpstreamClangTarget();

  protected abstract String getSpecPath();

  protected abstract String getSpikeTarget();

  private static Stream<String> inputFilesFromCFile() {
    return Arrays.stream(
            Objects.requireNonNull(new File("../../open-vadl/vadl-test/main/resources/llvm/riscv/spike")
                .listFiles()))
        .filter(File::isFile)
        .map(File::getName);
  }

  @EnabledIfEnvironmentVariable(named = "test.spike.enabled", matches = "true")
  @TestFactory
  List<DynamicTest> testSpike() throws IOException, DuplicatedPassKeyException {
    var doDebug = true;
    var target = getTarget();
    var upstreamBuildTarget = getUpstreamBuildTarget();
    var upstreamClangTarget = getUpstreamClangTarget();
    var configuration = new LcbConfiguration(getConfiguration(false),
        new ProcessorName(target));

    runLcb(configuration, getSpecPath());

    // Move Dockerfile into Docker Context
    {
      var inputStream = new FileInputStream(
          "../../open-vadl/vadl-test/main/resources/images/spike_" + getTarget() + "/Dockerfile");
      var outputStream = new FileOutputStream(configuration.outputPath() + "/lcb/Dockerfile");
      inputStream.transferTo(outputStream);
      outputStream.close();
    }

    var redisCache = getRunningRedisCache();
    var image = redisCache.setupEnv(new ImageFromDockerfile("tc_spike_riscv", !doDebug)
        .withDockerfile(Paths.get(configuration.outputPath() + "/lcb/Dockerfile"))
        .withBuildArg("TARGET", target)
        .withBuildArg("UPSTREAM_BUILD_TARGET", upstreamBuildTarget)
        .withBuildArg("UPSTREAM_CLANG_TARGET", upstreamClangTarget)
        .withBuildArg("SPIKE_TARGET", getSpikeTarget()));

    // The container is complete and has generated the assembly files.
    return inputFilesFromCFile().map(input -> DynamicTest.dynamicTest(input, () -> {
      runContainerWithEnv(image,
          Path.of("../../open-vadl/vadl-test/main/resources/llvm/riscv/spike"),
          "/src/inputs",
          "INPUT",
          input,
          doDebug);
    })).toList();
  }
}
