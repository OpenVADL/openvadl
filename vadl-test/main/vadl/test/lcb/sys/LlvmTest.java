package vadl.test.lcb.sys;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import vadl.test.AbstractTest;
import vadl.test.DockerExecutionTest;

public abstract class LlvmTest extends DockerExecutionTest {
  protected final String TEMP_DIR = System.getProperty("java.io.tmpdir");

  protected final String LLVM_BIN_DIR = System.getenv("LLVM_SOURCE_PATH") + "/build/bin";
  protected final String CLANG = LLVM_BIN_DIR + "/clang";
  protected final String LLC = LLVM_BIN_DIR + "/llc";

  protected String executeCommand(String... command) throws InterruptedException, IOException {
    System.out.println(
        "Issueing command: " + String.join(" ", command));
    Process llc = null;
    try {
      var x = new ProcessBuilder(command).inheritIO();
      llc = x.start();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    var llcOutput = new BufferedReader(new InputStreamReader(llc.getInputStream()));
    var result = llcOutput.lines().collect(Collectors.joining("\n"));

    Assertions.assertEquals(0, llc.waitFor());
    return result;
  }


}
