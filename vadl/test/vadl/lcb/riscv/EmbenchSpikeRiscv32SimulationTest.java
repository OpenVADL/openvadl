// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.lcb.riscv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.AbstractLcbTest;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.Pair;

public class EmbenchSpikeRiscv32SimulationTest extends AbstractLcbTest {

  @Test
  void runO0() throws DuplicatedPassKeyException, IOException {
    testEmbench(0);
  }

  @Test
  void runO3() throws DuplicatedPassKeyException, IOException {
    testEmbench(3);
  }

  void testEmbench(int optLevel) throws IOException, DuplicatedPassKeyException {
    var target = "rv32im";
    var upstreamBuildTarget = "RISCV";
    var doDebug = false;
    var configuration = new LcbConfiguration(getConfiguration(false),
        new ProcessorName(target));

    runLcb(configuration, "sys/risc-v/rv32im.vadl");

    // Move Dockerfile.riscv32.spike.lcb into Docker Context
    Files.createDirectories(Path.of(configuration.outputPath() + "/lcb/embench"));
    {
      var inputStream = new FileInputStream(
          "test/resources/images/spike_rv32im/Dockerfile");
      var outputStream =
          new FileOutputStream(configuration.outputPath() + "/lcb/Dockerfile");
      inputStream.transferTo(outputStream);
      outputStream.close();
    }

    // Copy embench
    {
      var input = new File(
          "test/resources/embench");
      var output = new File(configuration.outputPath() + "/lcb/embench");
      FileUtils.copyDirectory(input, output);
    }

    var redisCache = getRunningRedisCache();
    var cachedImage =
        EmbenchImageProvider.image(redisCache,
            configuration.outputPath() + "/lcb/Dockerfile",
            target, upstreamBuildTarget, doDebug);

    runContainerAndCopyInputIntoContainer(cachedImage,
        List.of(Pair.of(Path.of("../../open-vadl/vadl-test/main/resources/llvm/riscv/spike"),
            "/src/inputs")),
        Map.of(
            "LLVM_PARALLEL_COMPILE_JOBS", "4",
            "LLVM_PARALLEL_LINK_JOBS", "2"),
        "sh /src/embench/benchmark-extras/run-benchmarks-spike-clang-lcb-O" + optLevel + ".sh");
  }
}
