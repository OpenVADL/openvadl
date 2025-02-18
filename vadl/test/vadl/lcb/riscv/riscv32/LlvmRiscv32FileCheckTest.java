package vadl.lcb.riscv.riscv32;

import vadl.lcb.riscv.LlvmRiscvFileCheckTest;

public class LlvmRiscv32FileCheckTest extends LlvmRiscvFileCheckTest {
  @Override
  protected String getTarget() {
    return "rv32im";
  }

  @Override
  protected String getSpecPath() {
    return "sys/risc-v/rv32im.vadl";
  }

  @Override
  protected String getUpstreamBuildTarget() {
    return "RISCV";
  }

  @Override
  protected String getUpstreamClangTarget() {
    return "riscv32";
  }

  @Override
  protected String getSpikeTarget() {
    return "rv32im";
  }
}
