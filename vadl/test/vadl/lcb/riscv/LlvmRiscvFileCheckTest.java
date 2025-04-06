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
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.TargetName;
import vadl.lcb.AbstractLcbTest;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.Pair;

public abstract class LlvmRiscvFileCheckTest extends AbstractLcbTest {
  /*
    We also need all the spike methods because we do not know whether this test
    is run before the spike test. And therefore, the cached image must have the same vars.
   */
  protected abstract String getTarget();

  protected abstract String getUpstreamBuildTarget();

  protected abstract String getUpstreamClangTarget();

  protected abstract String getSpecPath();

  protected abstract String getSpikeTarget();

  private static Stream<String> inputFilesFromCFile(String target, int optLevel) {
    return Arrays.stream(
            Objects.requireNonNull(
                new File("test/resources/llvm/riscv/llvmIR/" + target + "/O"
                    + optLevel)
                    .listFiles()))
        .filter(File::isFile)
        .map(File::getName);
  }

  @TestFactory
  List<DynamicTest> compileLLvm() throws IOException, DuplicatedPassKeyException {
    var optLevelZero = run(0);
    var optLevelThree = run(3);

    return Stream.concat(optLevelZero.stream(), optLevelThree.stream()).toList();
  }

  private @Nonnull List<DynamicTest> run(int optLevel)
      throws IOException, DuplicatedPassKeyException {
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
            target, upstreamBuildTarget, upstreamClangTarget, getSpikeTarget(), false);

    return inputFilesFromCFile(target, optLevel).map(
        input -> DynamicTest.dynamicTest(input + " O" + optLevel, () -> {
          var name = Paths.get(input).getFileName().toString();

          runContainerAndCopyInputIntoContainer(cachedImage,
              List.of(
                  Pair.of(
                      Path.of(
                          "test/resources/llvm/riscv/llvmIR/" + target
                              + "/O" + optLevel),
                      "/src/inputs")
              ),
              Map.of("INPUT", name,
                  "OPT_LEVEL", optLevel + ""),
              "sh /work/filecheck.sh"
          );
        })).toList();
  }
}
