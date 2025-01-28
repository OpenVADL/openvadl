package vadl.test.iss;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.MountableFile;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.VadlFileUtils;

public class IssRV64IEmbenchTest extends QemuIssTest {

  private static final Logger log = LoggerFactory.getLogger(IssRV64IEmbenchTest.class);

  // TODO: Remove this (it is just for testing purposes)
  @EnabledIfEnvironmentVariable(named = "test_embench_enabled", matches = "true")
  @Test
  void issLoweringTest() throws IOException, DuplicatedPassKeyException {
    var image = generateIssSimulator("sys/risc-v/rv64i.vadl");

    // load embench from resources
    var embenchPath = VadlFileUtils.copyResourceDirToTempDir("embench", "embench");

    // build benchmarks for rv64i spike and run benchmarks in generated iss
    var runCommand = "chmod -R +x /work/embench "
        + "&& cd /work/embench "
        + "&& bash ./build_spike-rv64i.sh "
        + "&& bash ./benchmark_qemu.sh qemu-system-vadl -nographic -M virt -bios";

    runContainer(image,
        container -> container
            .withCopyFileToContainer(MountableFile.forHostPath(embenchPath), "/work/embench")
            .withCommand("/bin/bash", "-c", runCommand),
        null);
  }
}

