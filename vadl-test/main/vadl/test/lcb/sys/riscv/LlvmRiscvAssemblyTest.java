package vadl.test.lcb.sys.riscv;

import java.io.IOException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import vadl.test.lcb.sys.LlvmTest;

public class LlvmRiscvAssemblyTest extends LlvmTest {
  protected final static String TEST_DIR = System.getenv("RESOURCES_FOLDER") + "/llvm/riscv";
  protected final static String C_DIR =
      TEST_DIR + "/c";
  protected final static String IR_DIR =
      TEST_DIR + "/ir";
  protected final static String ASM_DIR =
      TEST_DIR + "/asm";

  @Test
  public void checkLlvm() throws InterruptedException, IOException {
    var cFile = C_DIR + "/ILP32_add.c";
    var llvmIrFile = IR_DIR + "/ILP32_add.ll";
    var asmFile = ASM_DIR + "/ILP32_add.s";

    executeCommand(CLANG, "-S", "-emit-llvm", "--target=rv64im", "-std=c11", "-o", llvmIrFile,
        cFile);
    executeCommand(LLC, "--march=rv64im", "-O=0", "-o", asmFile, "/tmp/temp.ll");

  }
}
