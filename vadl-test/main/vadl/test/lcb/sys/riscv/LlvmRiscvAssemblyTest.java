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
  protected final static String TEST_DIR = System.getenv("RESOURCES_FOLDER") + "/llvm/riscv";
  protected final static String BUCKET = System.getenv("BUCKET");
  protected final static String REGION = System.getenv("REGION");
  protected final static String AWS_ACCESS_KEY_ID = System.getenv("AWS_ACCESS_KEY_ID");
  protected final static String AWS_ACCESS_KEY = System.getenv("AWS_ACCESS_KEY");

  protected final static String C_DIR =
      TEST_DIR + "/c";
  protected final static String IR_DIR =
      TEST_DIR + "/ir";
  protected final static String ASM_DIR =
      TEST_DIR + "/asm";

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

    var image = new ImageFromDockerfile("tc_llvm17", false)
        .withDockerfile(Paths.get(configuration.outputPath() + "/lcb/Dockerfile"))
        .withBuildArg("BUCKET", BUCKET)
        .withBuildArg("REGION", REGION)
        .withBuildArg("AWS_ACCESS_KEY_ID", AWS_ACCESS_KEY_ID)
        .withBuildArg("AWS_ACCESS_KEY", AWS_ACCESS_KEY);

    runContainer(image, configuration.outputPath() + "/output", "/output");
  }
}
