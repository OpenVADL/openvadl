package vadl.lcb.asm;

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
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.riscv.SpikeRiscvImageProvider;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.Pair;

public abstract class AsmFileCheckTest extends AbstractLcbTest {

  protected abstract String getTarget();

  protected abstract String getUpstreamBuildTarget();

  protected abstract String getUpstreamClangTarget();

  protected abstract String getSpecPath();

  protected abstract String getSpikeTarget();


  @TestFactory
  List<DynamicTest> execute() throws IOException, DuplicatedPassKeyException {

    var target = getTarget();
    var upstreamBuildTarget = getUpstreamBuildTarget();
    var upstreamClangTarget = getUpstreamClangTarget();
    var configuration = new LcbConfiguration(getConfiguration(false),
        new ProcessorName(target));


    runLcb(configuration, getSpecPath());

    // Move Dockerfile into Docker Context
    {
      var inputStream = new FileInputStream(
          "test/resources/images/spike_" + getTarget() + "/Dockerfile");
      var outputStream = new FileOutputStream(configuration.outputPath() + "/lcb/Dockerfile");
      inputStream.transferTo(outputStream);
      outputStream.close();
    }

    var redisCache = getRunningRedisCache();

    var cachedImage =
        SpikeRiscvImageProvider.image(redisCache, configuration.outputPath() + "/lcb/Dockerfile",
            target, upstreamBuildTarget, upstreamClangTarget, getSpikeTarget(), false);

    return inputFilesFromFile(target).map(
        input -> DynamicTest.dynamicTest(input, () -> {
          var name = Paths.get(input).getFileName().toString();

          runContainerAndCopyInputIntoContainer(cachedImage,
              List.of(
                  Pair.of(
                      Path.of(
                          "test/resources/llvm/riscv/asm/" + target),
                      "/src/inputs")
              ),
              Map.of("INPUT", name),
              "sh /work/filecheck.sh"
          );
        })).toList();
  }

  private static Stream<String> inputFilesFromFile(String target) {
    return Arrays.stream(
            Objects.requireNonNull(
                new File("test/resources/llvm/riscv/asm/" + target)
                    .listFiles()))
        .filter(File::isFile)
        .map(File::getName);
  }
}
