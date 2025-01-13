package vadl.test.lcb.sys.riscv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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
  List<DynamicTest> testSpikeOptLevel0() throws IOException, DuplicatedPassKeyException {
    return run(0);
  }

  @EnabledIfEnvironmentVariable(named = "test.spike.enabled", matches = "true")
  @TestFactory
  List<DynamicTest> testSpikeOptLevel3() throws IOException, DuplicatedPassKeyException {
    return run(3);
  }

  private @Nonnull List<DynamicTest> run(int optLevel)
      throws IOException, DuplicatedPassKeyException {
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
    var cachedImage =
        SpikeRiscv32ImageProvider.image(redisCache, configuration.outputPath() + "/lcb/Dockerfile",
            target, upstreamBuildTarget, upstreamClangTarget, getSpikeTarget(), doDebug);

    // The container is complete and has generated the assembly files.
    return inputFilesFromCFile().map(
        input -> DynamicTest.dynamicTest(input + " with O" + optLevel,
            () -> runContainerWithEnv(cachedImage,
                Path.of("../../open-vadl/vadl-test/main/resources/llvm/riscv/spike"),
                "/src/inputs",
                Map.of("INPUT",
                    input,
                    "OPT_LEVEL", optLevel + "")))).toList();
  }
}
