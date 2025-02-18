package vadl.lcb.riscv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.AbstractLcbTest;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.Pair;

public class EmbenchSpikeRiscv32SimulationTest extends AbstractLcbTest {

  @Test
  void testEmbench() throws IOException, DuplicatedPassKeyException {
    var target = "rv32im";
    var upstreamBuildTarget = "RISCV";
    var configuration = new LcbConfiguration(getConfiguration(false),
        new ProcessorName(target));

    runLcb(configuration, "sys/risc-v/rv32im.vadl");

    // Move Dockerfile.riscv32.spike.lcb into Docker Context
    Files.createDirectories(Path.of(configuration.outputPath() + "/lcb/embench"));
    {
      var inputStream = new FileInputStream(
          "../../open-vadl/vadl-test/main/resources/images/spike_rv32im/Dockerfile");
      var outputStream =
          new FileOutputStream(configuration.outputPath() + "/lcb/Dockerfile");
      inputStream.transferTo(outputStream);
      outputStream.close();
    }

    // Copy embench
    {
      var input = new File(
          "../../open-vadl/vadl-test/main/resources/embench");
      var output = new File(configuration.outputPath() + "/lcb/embench");
      FileUtils.copyDirectory(input, output);
    }

    var redisCache = getRunningRedisCache();
    var image = redisCache.setupEnv(new ImageFromDockerfile("tc_embench_spike_riscv32")
        .withDockerfile(Paths.get(configuration.outputPath() + "/lcb/Dockerfile"))
        .withBuildArg("TARGET", target)
        .withBuildArg("UPSTREAM_BUILD_TARGET", upstreamBuildTarget));

    runContainerAndCopyInputIntoContainer(image,
        List.of(Pair.of(Path.of("../../open-vadl/vadl-test/main/resources/llvm/riscv/spike"),
            "/src/inputs")),
        Map.of(
            "LLVM_PARALLEL_COMPILE_JOBS", "4",
            "LLVM_PARALLEL_LINK_JOBS", "2"),
        "sh /src/embench/benchmark-extras/run-benchmarks-spike-clang-lcb.sh");
  }
}
