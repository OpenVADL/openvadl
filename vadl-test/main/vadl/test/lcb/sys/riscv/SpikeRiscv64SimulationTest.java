package vadl.test.lcb.sys.riscv;

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
  protected String getUpstreamBuildTarget() {
    return "RISCV";
  }

  @Override
  protected String getUpstreamClangTarget() {
    return "riscv64";
  }

  @Override
  protected String getSpikeTarget() {
    return "rv64gc";
  }
}
