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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.shaded.org.apache.commons.io.file.DirectoryStreamFilter;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.lcb.AbstractLcbTest;

public class LlvmRiscvAssemblyTest extends AbstractLcbTest {

  private static Stream<String> inputFilesFromCFile() {
    return Arrays.stream(
            Objects.requireNonNull(new File("../../open-vadl/vadl-test/main/resources/llvm/riscv/c")
                .listFiles()))
        .filter(File::isFile)
        .map(File::getName);
  }

  @EnabledIfEnvironmentVariable(named = "test.llvm.enabled", matches = "true")
  @TestFactory
  List<DynamicTest> compileLlvm() throws IOException, DuplicatedPassKeyException {
    var target = "rv64im";
    var configuration = new LcbConfiguration(getConfiguration(false),
        new ProcessorName(target));

    runLcb(configuration, "sys/risc-v/rv64im.vadl");

    // Move Dockerfile into Docker Context
    {
      var inputStream = new FileInputStream(
          "../../open-vadl/vadl-test/main/resources/images/llvm_riscv/Dockerfile");
      var outputStream = new FileOutputStream(configuration.outputPath() + "/lcb/Dockerfile");
      inputStream.transferTo(outputStream);
      outputStream.close();
    }

    var image = SetupRedisEnv.setupEnv(new ImageFromDockerfile("tc_llvm17", false)
            .withDockerfile(Paths.get(configuration.outputPath() + "/lcb/Dockerfile"))
            .withBuildArg("TARGET", target))
        .withBuildImageCmdModifier(modifier -> modifier.withNetworkMode(testNetwork().getId()));

    var hostOutput = "build/output/llvm";
    new File(hostOutput).mkdirs();

    runContainerWithContent(image,
        "../../open-vadl/vadl-test/main/resources/llvm/riscv/c",
        "/src/inputs",
        new File(hostOutput).getAbsolutePath(),
        "/output");

    // The container is complete and has generated the assembly files.
    return inputFilesFromCFile().map(input -> DynamicTest.dynamicTest(input, () -> {
      var name = Paths.get(input).getFileName();
      var expected = new File(
          "../../open-vadl/vadl-test/main/resources/llvm/riscv/assertions/assembly/" + name + ".s");

      Arrays.stream(new File(hostOutput).listFiles())
          .forEach(x -> System.out.println(x.getAbsolutePath()));

      var actual = new File(hostOutput + "/" + name + ".s");
      assertThat(contentOf(actual)).isEqualToIgnoringWhitespace(contentOf(expected));
    })).toList();
  }
}
