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

package vadl.iss.riscv;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.MountableFile;
import vadl.iss.QemuIssTest;
import vadl.utils.VadlFileUtils;

public class IssRiscvEmbenchTest extends QemuIssTest {

  private static final Logger log = LoggerFactory.getLogger(IssRiscvEmbenchTest.class);

  @Override
  protected List<String> withUpstreamTargets() {
    return List.of("riscv64-softmmu", "riscv32-softmmu");
  }

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

