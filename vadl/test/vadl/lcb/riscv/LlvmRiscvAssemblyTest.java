package vadl.lcb.riscv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.lcb.AbstractLcbTest;
import vadl.utils.Pair;

public abstract class LlvmRiscvAssemblyTest extends AbstractLcbTest {
  /*
    We also need all the spike methods because we do not know whether this test
    is run before the spike test. And therefore, the cached image must have the same vars.
   */
  protected abstract String getTarget();

  protected abstract String getUpstreamBuildTarget();

  protected abstract String getUpstreamClangTarget();

  protected abstract String getSpecPath();

  protected abstract String getSpikeTarget();

  private static Stream<String> inputFilesFromCFile() {
    return Arrays.stream(
            Objects.requireNonNull(new File("../../open-vadl/vadl-test/main/resources/llvm/riscv/c")
                .listFiles()))
        .filter(File::isFile)
        .map(File::getName);
  }

  @EnabledIfEnvironmentVariable(named = "test_llvm_enabled", matches = "true")
  @TestFactory
  List<DynamicTest> compileLlvm() throws IOException, DuplicatedPassKeyException {
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
        SpikeRiscvImageProvider.image(redisCache, configuration.outputPath() + "/lcb/Dockerfile",
            target, upstreamBuildTarget, upstreamClangTarget, getSpikeTarget(), false);

    // The container is complete and has generated the assembly files.
    return inputFilesFromCFile().map(input -> DynamicTest.dynamicTest(input, () -> {
      var name = Paths.get(input).getFileName().toString();

      runContainerAndCopyInputIntoContainer(cachedImage,
          List.of(
              Pair.of(
                  Path.of("../../open-vadl/vadl-test/main/resources/llvm/riscv/c"),
                  "/src/inputs"),
              Pair.of(
                  Path.of("../../open-vadl/vadl-test/main/resources/llvm/riscv/assertions/"
                      + getTarget()),
                  "/assertions")
          ),
          Map.of("INPUT", name),
          "sh /work/llvm.sh"
      );
    })).toList();
  }
}
