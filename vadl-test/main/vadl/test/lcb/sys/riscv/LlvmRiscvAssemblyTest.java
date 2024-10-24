package vadl.test.lcb.sys.riscv;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.lcb.AbstractLcbTest;

public class LlvmRiscvAssemblyTest extends AbstractLcbTest {
  @EnabledIfEnvironmentVariable(named = "test.llvm.enabled", matches = "true")
  @Test
  void compileLlvm() throws IOException, DuplicatedPassKeyException {
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

    // Move test into tempdir
    {
      var inputStream =
          new FileInputStream("../../open-vadl/vadl-test/main/resources/llvm/riscv/c/ILP32_add.c");
      var outputStream = new FileOutputStream(configuration.outputPath() + "/lcb/input.c");
      inputStream.transferTo(outputStream);
      outputStream.close();
    }

    var image = SetupRedisEnv.setupEnv(new ImageFromDockerfile("tc_llvm17")
            .withDockerfile(Paths.get(configuration.outputPath() + "/lcb/Dockerfile"))
            .withBuildArg("TARGET", target))
        .withBuildImageCmdModifier(modifier -> modifier.withNetworkMode(testNetwork().getId()));

    runContainer(image);
  }
}
