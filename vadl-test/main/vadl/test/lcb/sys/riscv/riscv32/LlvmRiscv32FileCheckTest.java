package vadl.test.lcb.sys.riscv.riscv32;

import vadl.test.lcb.sys.riscv.LlvmRiscvFileCheckTest;

public class LlvmRiscv32FileCheckTest extends LlvmRiscvFileCheckTest {

  @Override
  protected String getTarget() {
    return "rv32im";
  }

  @Override
  protected String getSpecPath() {
    return "sys/risc-v/rv32im.vadl";
  }
}
