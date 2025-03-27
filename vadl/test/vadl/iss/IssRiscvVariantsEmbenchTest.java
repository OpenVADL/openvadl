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

package vadl.iss;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.utils.VadlFileUtils;
import vadl.configuration.IssConfiguration;

public class IssRiscvVariantsEmbenchTest extends QemuIssTest {

  private static final Logger log = LoggerFactory.getLogger(IssRiscvEmbenchTest.class);

  @Test
  void rv64imVariantsEmbenchTest() throws IOException {
    String vadlPath = "sys/risc-v/rv64im.vadl";
    runEmbenchTest(vadlPath, EnumSet.noneOf(IssConfiguration.IssOptsToSkip.class), "-no-skip");
    runEmbenchTest(vadlPath, EnumSet.of(IssConfiguration.IssOptsToSkip.OPT_VAR_ALLOC), "-var-alloc");
    runEmbenchTest(vadlPath, EnumSet.of(IssConfiguration.IssOptsToSkip.OPT_ARGS), "-args");
    runEmbenchTest(vadlPath, EnumSet.of(IssConfiguration.IssOptsToSkip.OPT_JMP_SLOTS), "-jmp-slots");
  }

  private void runEmbenchTest(String vadlPath, EnumSet<IssConfiguration.IssOptsToSkip> optsToSkip, String postfix)
      throws IOException {

    var config = IssConfiguration.from(getConfiguration(false));

    config.setOptsToSkip(optsToSkip);

    ConcurrentHashMap<String, ImageFromDockerfile> emptyCache = new ConcurrentHashMap<>();
    var image = generateSimulator(emptyCache, vadlPath, config);

    // Load embench from resources
    var embenchPath = VadlFileUtils.copyResourceDirToTempDir("embench", "embench");

    // Build benchmarks and run them in the generated ISS
    var runCommand = String.format(
          "chmod -R +x /work/embench && cd /work/embench "
          + "&& apt-get update "
          + "&& DEBIAN_FRONTEND=noninteractive TZ=Etc/UTC apt-get install -y python3-pandas python3-numpy python3-scipy"
          + "&& bash ./benchmark-extras/run-benchmarks-rv64im.sh"
        );

    var resultsDir = getTestDirectory().resolve("/tmp/iss-results" + postfix);

    runContainer(image,
        container -> container
            .withCopyFileToContainer(MountableFile.forHostPath(embenchPath), "/work/embench")
            .withCommand("/bin/bash", "-c", runCommand),
        container ->
            copyPathFromContainer(container, "/work/embench/benchmark-extras/results", resultsDir)
    );
  }

}

