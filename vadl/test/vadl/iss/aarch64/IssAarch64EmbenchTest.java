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

package vadl.iss.aarch64;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.MountableFile;
import vadl.iss.QemuIssTest;
import vadl.iss.riscv.IssRiscvEmbenchTest;
import vadl.utils.VadlFileUtils;

public class IssAarch64EmbenchTest extends QemuIssTest {

  private static final Logger log = LoggerFactory.getLogger(IssRiscvEmbenchTest.class);

  @Test
  void a64EmbenchTest() throws IOException {
    runEmbenchTest("sys/aarch64/virt.vadl", "build_virt-iss-a64.sh", "qemu-system-a64");
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
            + "&& bash ./benchmark_qemu.sh %s -nographic -bios",
        buildScript, qemuSystem
    );

    runContainer(image,
        container -> container
            .withCopyFileToContainer(MountableFile.forHostPath(embenchPath), "/work/embench")
            .withCommand("/bin/bash", "-c", runCommand),
        null);
  }
}
