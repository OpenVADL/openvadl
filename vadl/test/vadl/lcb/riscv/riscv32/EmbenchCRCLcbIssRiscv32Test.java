// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.lcb.riscv.riscv32;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import vadl.DockerImage;
import vadl.configuration.IssConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.TargetName;
import vadl.iss.TestConstants;
import vadl.lcb.LcbDockerExecutionTest;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.Pair;

/**
 * Run the Embench CRC and adjusted CRC DSE tests with LCB and ISS for the rv32icustom spec.
 */
public class EmbenchCRCLcbIssRiscv32Test extends LcbDockerExecutionTest {

  @Override
  protected String getTarget() {
    return "rv32im";
  }

  @Override
  protected String getUpstreamBuildTarget() {
    return "RISCV";
  }

  @Override
  protected String getUpstreamClangTarget() {
    return "riscv32";
  }

  @Override
  protected String getSpikeTarget() {
    return "rv64im";
  }

  @Override
  protected String getAbi() {
    return "ilp32";
  }

  @Override
  protected LcbConfiguration getConfiguration() {
    return new LcbConfiguration(getConfiguration(false), new TargetName("rv32icustomcrc1"));
  }

  @Test
  public void testEmbench() throws IOException, DuplicatedPassKeyException {
    var cmd = "bash /src/embench/benchmark-extras/rv32-run-benchmarks-spike-lcb-iss-O3.sh";
    run("sys/risc-v/rv32iCustom.vadl", cmd);
  }

  @Override
  protected void run(String specPath, String cmd, Map<String, String> environments)
      throws DuplicatedPassKeyException, IOException {
    var lcbConfiguration = getConfiguration();
    runLcb(lcbConfiguration, specPath);
    copyIntoDockerContext(lcbConfiguration);

    // Use a fresh specification: both pass orders mutate their VIAM input.
    var issConfiguration = IssConfiguration.from(getConfiguration(false));
    setupPassManagerAndRunSpec(specPath, PassOrders.iss(issConfiguration));
    var target = issConfiguration.targetName().toLowerCase(Locale.ROOT);
    var lcbPath = lcbConfiguration.outputPath().resolve("lcb");
    var issPath = issConfiguration.outputPath().resolve("iss");

    // Compile the ISS in the LCB image so its runtime libraries match the test container.
    var dockerfile = """
        # syntax=docker/dockerfile:1.7
        FROM %s AS qemu-source
        """.formatted(TestConstants.TEST_BASE_IMAGE)
        + Files.readString(lcbPath.resolve("Dockerfile")) + """

        USER root
        RUN apt-get update && apt-get install -y --no-install-recommends \
            build-essential libglib2.0-dev libfdt-dev libpixman-1-dev zlib1g-dev \
            ninja-build python3-venv python3-pip \
            && rm -rf /var/lib/apt/lists/*

        COPY --from=qemu-source /qemu /openvadl-iss
        COPY openvadl-iss /openvadl-iss
        WORKDIR /openvadl-iss/build
        RUN ../configure --cc='sccache gcc' --prefix=/opt/openvadl-iss --target-list=%s-softmmu
        RUN --mount=type=cache,target=/root/.cache/sccache \
            sccache --start-server && make -j4 && make install && sccache -s
        WORKDIR /src
        """.formatted(target);

    var image = new DockerImage()
        .withDockerfile(dockerfile)
        .withFileFromPath(".", lcbPath)
        .withFileFromPath("openvadl-iss", issPath)
        .withBuildArg("TARGET", getTarget())
        .withBuildArg("UPSTREAM_BUILD_TARGET", getUpstreamBuildTarget())
        .withBuildArg("UPSTREAM_CLANG_TARGET", getUpstreamClangTarget())
        .withBuildArg("SPIKE_TARGET", getSpikeTarget())
        .withBuildArg("ABI", getAbi());

    var environment = new HashMap<>(environments);
    environment.put("QEMU_SYSTEM", "/opt/openvadl-iss/bin/qemu-system-" + target);
    environment.put("QEMU_MACHINE", issConfiguration.machineName().toLowerCase(Locale.ROOT));
    runContainerAndCopyInputIntoContainer(image,
        List.of(Pair.of(Path.of("../../open-vadl/vadl-test/main/resources/llvm/riscv/spike"),
            "/src/inputs")), environment, cmd);
  }
}

