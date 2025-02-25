package vadl.iss;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.MountableFile;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.VadlFileUtils;

public class IssRiscvEmbenchTest extends QemuIssTest {

  @Test
  void rv64imEmbenchTest() throws IOException {
    var image = generateIssSimulator("sys/risc-v/rv64im.vadl");

    // load embench from resources
    var embenchPath = VadlFileUtils.copyResourceDirToTempDir("embench", "embench");

    // build benchmarks for rv64i spike and run benchmarks in generated iss
    var runCommand = "chmod -R +x /work/embench "
        + "&& cd /work/embench "
        + "&& bash ./build_spike-rv64im.sh "
        + "&& bash ./benchmark_qemu.sh qemu-system-rv64im -nographic -M virt -bios";

    runContainer(image,
        container -> container
            .withCopyFileToContainer(MountableFile.forHostPath(embenchPath), "/work/embench")
            .withCommand("/bin/bash", "-c", runCommand),
        null);
  }


  @Test
  void rv32imEmbenchTest() throws IOException {
    var image = generateIssSimulator("sys/risc-v/rv32im.vadl");

    // load embench from resources
    var embenchPath = VadlFileUtils.copyResourceDirToTempDir("embench", "embench");

    // build benchmarks for rv64i spike and run benchmarks in generated iss
    var runCommand = "chmod -R +x /work/embench "
        + "&& cd /work/embench "
        + "&& bash ./build_spike-rv32im.sh "
        + "&& bash ./benchmark_qemu.sh qemu-system-rv32im -nographic -M virt -bios";

    runContainer(image,
        container -> container
            .withCopyFileToContainer(MountableFile.forHostPath(embenchPath), "/work/embench")
            .withCommand("/bin/bash", "-c", runCommand),
        null);
  }

}

