package vadl.lcb.riscv.riscv64;

import vadl.lcb.riscv.SpikeRiscvSimulationTest;

public class SpikeRiscv64SimulationTest extends SpikeRiscvSimulationTest {
  @Override
  protected String getTarget() {
    return "rv64im";
  }

  @Override
  protected String getSpecPath() {
    return "sys/risc-v/rv64im.vadl";
  }

  @Override
  protected String getSpikeTarget() {
    return "rv64gc";
  }

  @Override
  protected String getUpstreamBuildTarget() {
    return "RISCV";
  }

  @Override
  protected String getUpstreamClangTarget() {
    return "riscv64";
  }
}
