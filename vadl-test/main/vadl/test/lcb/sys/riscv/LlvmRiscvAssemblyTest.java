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
  protected final String bucket = System.getenv("BUCKET");
  protected final String region = System.getenv("REGION");
  protected final String awsAccessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
  protected final String awsAccessKey = System.getenv("AWS_ACCESS_KEY");

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

    var image = new ImageFromDockerfile("tc_llvm17")
        .withDockerfile(Paths.get(configuration.outputPath() + "/lcb/Dockerfile"))
        .withBuildArg("BUCKET", bucket)
        .withBuildArg("REGION", region)
        .withBuildArg("AWS_ACCESS_KEY_ID", awsAccessKeyId)
        .withBuildArg("AWS_ACCESS_KEY", awsAccessKey);

    runContainer(image, configuration.outputPath() + "/output", "/output");
  }
}
