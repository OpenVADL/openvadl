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

package vadl.lcb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.TargetName;
import vadl.lcb.riscv.SpikeRiscvImageProvider;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.Pair;

public abstract class LcbDockerInputFileExecutionTest extends LcbDockerExecutionTest {
  /**
   * Return a stream of file names from a given {@code sourceDirectory}. This method will
   * not return the result of a subdirectory but only exactly the {@code sourceDirectory}.
   * If the {@code sourceDirectory} does not exist then return nothing. This method will
   * *not* throw an exception if the directory does not exist.
   */
  protected Stream<String> inputFiles(String sourceDirectory) {
    // Return nothing when directory does not exist.
    if (!Files.exists(Path.of(sourceDirectory))) {
      return Stream.empty();
    }

    return Arrays.stream(
            Objects.requireNonNull(
                new File(sourceDirectory)
                    .listFiles()))
        .filter(File::isFile)
        .map(File::getName);
  }

  protected List<DynamicTest> runEach(String specPath,
                                      String sourceDirectory,
                                      int optLevel,
                                      String cmd) throws DuplicatedPassKeyException, IOException {
    return runEach(specPath, List.of(sourceDirectory), optLevel, cmd);
  }

  protected List<DynamicTest> runEach(String specPath, List<String> sourceDirectories, int optLevel,
                                      String cmd)
      throws DuplicatedPassKeyException,
      IOException {
    var doDebug = false;
    var configuration = new LcbConfiguration(getConfiguration(false),
        new TargetName(getTarget()));

    runLcb(configuration, specPath);
    copyIntoDockerContext(configuration);

    var redisCache = getRunningRedisCache();
    var cachedImage =
        SpikeRiscvImageProvider.image(redisCache,
            configuration.outputPath() + "/lcb/Dockerfile",
            getTarget(),
            getUpstreamBuildTarget(),
            getUpstreamClangTarget(),
            getSpikeTarget(),
            getAbi(),
            doDebug);

    return sourceDirectories.stream()
        .map(sourceDirectory -> Pair.of(sourceDirectory, inputFiles(sourceDirectory))).flatMap(
            pair -> {
              var sourceDirectory = pair.left();
              var inputs = pair.right();
              return inputs.map(
                  input -> DynamicTest.dynamicTest(input + " O" + optLevel, () -> {
                    var name = Paths.get(input).getFileName().toString();

                    runContainerAndCopyInputIntoContainer(cachedImage,
                        List.of(
                            Pair.of(
                                Path.of(sourceDirectory),
                                "/src/inputs")
                        ),
                        Map.of("INPUT", name,
                            "OPT_LEVEL", optLevel + ""),
                        cmd
                    );
                  }));
            })
        .toList();
  }
}
