package vadl.iss;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.MountableFile;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.VadlFileUtils;

public class IssRiscvEmbenchTest extends QemuIssTest {

  private static final Logger log = LoggerFactory.getLogger(IssRiscvEmbenchTest.class);

  @Test
  void rv64imEmbenchTest() throws IOException {
    runEmbenchTest("sys/risc-v/rv64im.vadl", "build_spike-rv64im.sh", "qemu-system-rv64im");
  }

  @Test
  void rv32imEmbenchTest() throws IOException {
    runEmbenchTest("sys/risc-v/rv32im.vadl", "build_spike-rv32im.sh", "qemu-system-rv32im");
  }

  private void runEmbenchTest(String vadlPath, String buildScript, String qemuSystem)
      throws IOException {
    var image = generateIssSimulator(vadlPath);

    var mhz = 50;
    var envMhz = System.getenv("EMBENCH_MHZ");
    if (envMhz != null) {
      mhz = Integer.parseInt(envMhz);
    }
    
    log.info("Setting up embench with {} mhz", mhz);

    // Load embench from resources
    var embenchPath = VadlFileUtils.copyResourceDirToTempDir("embench", "embench");

    // Build benchmarks and run them in the generated ISS
    var runCommand = String.format(
        "chmod -R +x /work/embench && cd /work/embench "
            + "&& bash ./%s --cpu-mhz=" + mhz
            + "&& bash ./benchmark_qemu.sh %s -nographic -M virt -bios",
        buildScript, qemuSystem
    );

    runContainer(image,
        container -> container
            .withCopyFileToContainer(MountableFile.forHostPath(embenchPath), "/work/embench")
            .withCommand("/bin/bash", "-c", runCommand),
        null);
  }

}

