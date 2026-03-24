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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.TargetName;
import vadl.lcb.LcbDockerExecutionTest;
import vadl.pass.exception.DuplicatedPassKeyException;

public abstract class AbstractLcbBenchmarkTest extends LcbDockerExecutionTest {
  @Override
  protected void run(String specPath, String cmd, Map<String, String> environments)
      throws DuplicatedPassKeyException, IOException {

    var hostPath = System.getenv("EMBENCH_BENCHMARK_RESULT_HOST_PATH");
    var guestPath = System.getenv("EMBENCH_BENCHMARK_RESULT_GUEST_PATH");

    var configuration = new LcbConfiguration(getConfiguration(false),
        new TargetName(getTarget()));

    runLcb(configuration, specPath);
    copyIntoDockerContext(configuration);

    var cachedImage =
        DockerRiscvImageProvider.image(
            configuration.outputPath() + "/lcb/Dockerfile",
            getTarget(),
            getImageName(),
            getUpstreamBuildTarget(),
            getUpstreamClangTarget(),
            getSpikeTarget(),
            getAbi());

    runContainerAndCopyDirectoryIntoContainerAndCopyOutputBack(cachedImage,
        List.of(),
        hostPath,
        guestPath,
        cmd);
  }
}
