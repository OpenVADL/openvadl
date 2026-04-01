// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.valuetypes.TargetName;
import vadl.lcb.riscv.DockerRiscvImageProvider;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.Pair;

public abstract class LcbDockerExecutionTest extends AbstractLcbTest {
  protected abstract String getTarget();

  protected abstract String getUpstreamBuildTarget();

  protected abstract String getUpstreamClangTarget();

  protected abstract String getSpikeTarget();

  protected abstract String getAbi();

  private void copyLcbExecutionTestIntoDockerContext(LcbConfiguration configuration)
      throws IOException {
    Files.createDirectories(Path.of(configuration.outputPath() + "/lcb/embench"));
    var inputStream = new FileInputStream(
        "resources/images/lcb_execution_test_" + getTarget() + "/Dockerfile");
    var outputStream =
        new FileOutputStream(configuration.outputPath() + "/lcb/Dockerfile");
    inputStream.transferTo(outputStream);
    outputStream.close();
  }

  private void copyEmbenchIntoDockerContext(LcbConfiguration configuration) throws IOException {
    var input = new File(
        "resources/embench");
    var output = new File(configuration.outputPath() + "/lcb/embench");
    FileUtils.copyDirectory(input, output);
  }

  protected void copyIntoDockerContext(LcbConfiguration configuration) throws IOException {
    copyLcbExecutionTestIntoDockerContext(configuration);
    copyEmbenchIntoDockerContext(configuration);
  }

  protected Map<String, String> defaultEnvironment() {
    var map = new HashMap<String, String>();
    map.put("LLVM_PARALLEL_COMPILE_JOBS", "4");
    map.put("LLVM_PARALLEL_LINK_JOBS", "2");
    return map;
  }

  protected LcbConfiguration getConfiguration() {
    return new LcbConfiguration(getConfiguration(false), new TargetName(getTarget()));
  }

  /**
   * Get the name of the docker image name for this test, usually the image is the target name.
   * But in some cases it is necessary to create multiple images for the same target.
   */
  protected String getImageName() {
    return getTarget();
  }

  protected void run(String specPath, String cmd) throws DuplicatedPassKeyException, IOException {
    var environment = defaultEnvironment();
    run(specPath, cmd, environment);
  }

  protected void run(String specPath, String cmd, Map<String, String> environments)
      throws DuplicatedPassKeyException, IOException {
    var configuration = getConfiguration();

    runLcb(configuration, specPath);
    copyIntoDockerContext(configuration);

    var cachedImage =
        DockerRiscvImageProvider.image(
            configuration.outputPath() + "/lcb/Dockerfile",
            getImageName(),
            getTarget(),
            getUpstreamBuildTarget(),
            getUpstreamClangTarget(),
            getSpikeTarget(),
            getAbi());

    runContainerAndCopyInputIntoContainer(cachedImage,
        List.of(Pair.of(Path.of("../../open-vadl/vadl-test/main/resources/llvm/riscv/spike"),
            "/src/inputs")),
        environments,
        cmd);
  }
}
