package vadl.test.lcb.sys.riscv;

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
import javax.annotation.Nonnull;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.shaded.com.google.common.collect.Streams;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.lcb.AbstractLcbTest;
import vadl.utils.Pair;

public abstract class LlvmRiscvFileCheckTest extends AbstractLcbTest {

  protected abstract String getTarget();

  protected abstract String getSpecPath();

  private static Stream<String> inputFilesFromCFile(String target, int optLevel) {
    return Arrays.stream(
            Objects.requireNonNull(
                new File("../../open-vadl/vadl-test/main/resources/llvm/riscv/llvmIR/" + target + "/O"
                    + optLevel)
                    .listFiles()))
        .filter(File::isFile)
        .map(File::getName);
  }

  @EnabledIfEnvironmentVariable(named = "test.llvm.enabled", matches = "true")
  @TestFactory
  List<DynamicTest> compileLLvm() throws IOException, DuplicatedPassKeyException {
    var optLevelZero = run(0);
    var optLevelThree = run(3);

    return Stream.concat(optLevelZero.stream(), optLevelThree.stream()).toList();
  }

  private @Nonnull List<DynamicTest> run(int optLevel)
      throws IOException, DuplicatedPassKeyException {
    var target = getTarget();
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

    /* These variables are not required for LLVM assembly test. */
    var upstreamBuildTarget = "";
    var upstreamClangTarget = "";
    var spikeTarget = "";

    var cachedImage =
        SpikeRiscvImageProvider.image(redisCache, configuration.outputPath() + "/lcb/Dockerfile",
            target, upstreamBuildTarget, upstreamClangTarget, spikeTarget, false);

    return inputFilesFromCFile(target, optLevel).map(
        input -> DynamicTest.dynamicTest(input + " O" + target, () -> {
          var name = Paths.get(input).getFileName().toString();

          runContainerAndCopyInputIntoContainer(cachedImage,
              List.of(
                  Pair.of(
                      Path.of(
                          "../../open-vadl/vadl-test/main/resources/llvm/riscv/llvmIR/" + target
                              + "/O" + optLevel),
                      "/src/inputs")
              ),
              Map.of("INPUT", name,
                  "OPT_LEVEL", optLevel + ""),
              "sh /work/filecheck.sh"
          );
        })).toList();
  }
}
