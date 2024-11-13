package vadl.test.lcb.sys.riscv;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.contentOf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.lcb.AbstractLcbTest;

public class SpikeRiscv64SimulationTest extends AbstractLcbTest {

  private static Stream<String> inputFilesFromCFile() {
    return Arrays.stream(
            Objects.requireNonNull(new File("../../open-vadl/vadl-test/main/resources/llvm/riscv/c")
                .listFiles()))
        .filter(File::isFile)
        .map(File::getName);
  }

  @TestFactory
  List<DynamicTest> testFiles() throws IOException, DuplicatedPassKeyException {
    var target = "rv64im";
    var upstreamBuildTarget = "RISCV";
    var upstreamClangTarget = "riscv64";
    var configuration = new LcbConfiguration(getConfiguration(false),
        new ProcessorName(target));

    runLcb(configuration, "sys/risc-v/rv64im.vadl");

    // Move Dockerfile into Docker Context
    {
      var inputStream = new FileInputStream(
          "../../open-vadl/vadl-test/main/resources/images/spike_riscv64/Dockerfile");
      var outputStream = new FileOutputStream(configuration.outputPath() + "/lcb/Dockerfile");
      inputStream.transferTo(outputStream);
      outputStream.close();
    }

    var redisCache = getRunningRedisCache();
    var image = redisCache.setupEnv(new ImageFromDockerfile("tc_spike_riscv64")
        .withDockerfile(Paths.get(configuration.outputPath() + "/lcb/Dockerfile"))
        .withBuildArg("TARGET", target)
        .withBuildArg("UPSTREAM_BUILD_TARGET", upstreamBuildTarget)
        .withBuildArg("UPSTREAM_CLANG_TARGET", upstreamClangTarget)
        .withBuildArg("SPIKE_TARGET", "rv64gc"));

    // We build the image and copy all the input files into the container.
    // The llvm compiler compiles all assembly files, and we copy them from the container
    // to the host (hostOutput folder).
    var hostOutput = configuration.outputPath() + "/output/";

    runContainerAndCopyInputIntoAndCopyOutputFromContainer(image,
        Path.of("../../open-vadl/vadl-test/main/resources/llvm/riscv/spike"),
        "/src/inputs",
        Path.of(hostOutput),
        "/output");

    // The container is complete and has generated the assembly files.
    return inputFilesFromCFile().map(input -> DynamicTest.dynamicTest(input, () -> {
      var name = Paths.get(input).getFileName();
      var errorPath = hostOutput + name + ".out";
      var errorFile = new File(errorPath);

      // First check if an error file exists. Note that the container always
      // creates an error file, so we also check for the size.
      if (errorFile.exists() && errorFile.length() != 0) {
        assertThat(contentOf(errorFile)).isEqualToIgnoringWhitespace("");
      }
    })).toList();
  }
}
