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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.gcb.valuetypes.TargetName;
import vadl.lcb.AbstractLcbTest;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.Pair;

public abstract class SpikeRiscvSimulationTest extends AbstractLcbTest {
  protected abstract String getTarget();

  protected abstract String getUpstreamBuildTarget();

  protected abstract String getUpstreamClangTarget();

  protected abstract String getSpecPath();

  protected abstract String getSpikeTarget();

  protected abstract String getAbi();

  private static Stream<String> inputFilesFromCFile() {
    return Arrays.stream(
            Objects.requireNonNull(new File("test/resources/llvm/riscv/spike")
                .listFiles()))
        .filter(File::isFile)
        .map(File::getName);
  }

  @TestFactory
  List<DynamicTest> testSpikeOptLevel0() throws IOException, DuplicatedPassKeyException {
    return run(0);
  }

  @TestFactory
  List<DynamicTest> testSpikeOptLevel3() throws IOException, DuplicatedPassKeyException {
    return run(3);
  }

  private @Nonnull List<DynamicTest> run(int optLevel)
      throws IOException, DuplicatedPassKeyException {
    var doDebug = false;
    var target = getTarget();
    var upstreamBuildTarget = getUpstreamBuildTarget();
    var upstreamClangTarget = getUpstreamClangTarget();
    var configuration = new LcbConfiguration(getConfiguration(false),
        new TargetName(target));

    runLcb(configuration, getSpecPath());

    // Move Dockerfile into Docker Context
    {
      var inputStream = new FileInputStream(
          "test/resources/images/spike_" + getTarget() + "/Dockerfile");
      var outputStream = new FileOutputStream(configuration.outputPath() + "/lcb/Dockerfile");
      inputStream.transferTo(outputStream);
      outputStream.close();
    }

    var redisCache = getRunningRedisCache();
    var cachedImage =
        SpikeRiscvImageProvider.image(redisCache, configuration.outputPath() + "/lcb/Dockerfile",
            target, upstreamBuildTarget, upstreamClangTarget, getSpikeTarget(), getAbi(), doDebug);

    // The container is complete and has generated the assembly files.
    return inputFilesFromCFile().map(input -> DynamicTest.dynamicTest(input, () -> {
      runContainerAndCopyInputIntoContainer(cachedImage,
          List.of(Pair.of(Path.of("test/resources/llvm/riscv/spike"),
              "/src/inputs")),
          Map.of(
              "OPT_LEVEL", optLevel + "",
              "INPUT",
              input,
              "LLVM_PARALLEL_COMPILE_JOBS", "4",
              "LLVM_PARALLEL_LINK_JOBS", "2"));
    })).toList();
  }
}
