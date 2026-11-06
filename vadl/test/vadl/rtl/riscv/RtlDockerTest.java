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

package vadl.rtl.riscv;

import static vadl.rtl.passes.EmitRtlDevcontainerDockerComposePass.RTL_BASE_IMAGE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.testcontainers.utility.MountableFile;
import vadl.DockerExecutionTest;
import vadl.configuration.RtlConfiguration;
import vadl.pass.PassManager;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.rtl.ipg.nodes.RtlDecodeTreeNode;
import vadl.rtl.passes.RtlConfigurationPass;
import vadl.utils.Pair;
import vadl.viam.Stage;

public class RtlDockerTest extends DockerExecutionTest {

  /**
   * Generate RTL for a specification and run a command in the rtl-test-base container with access
   * to the generated RTL.
   *
   * @param specPath specification file to generate
   * @param macroOverrides macro overrides
   * @param config configuration
   * @param resultMappings results files to copy back from the container
   * @param cmd command to execute
   */
  protected void runContainerWithSpec(String specPath, Map<String, String> macroOverrides,
                                      RtlConfiguration config,
                                      List<Pair<String, String>> resultMappings,
                                      String... cmd) {
    // Generate RTL core
    try {
      setupPassManagerAndRunSpec(specPath, PassOrders.rtl(config), macroOverrides);
    } catch (IOException | DuplicatedPassKeyException e) {
      throw new RuntimeException(e);
    }

    // Input files
    var outputPath = Path.of(config.outputPath() + "/rtl").toAbsolutePath();
    if (!outputPath.toFile().exists()) {
      throw new IllegalStateException("RTL output path was not found (not generated?)");
    }

    runContainer(
        RTL_BASE_IMAGE,
        c -> {
          c.setCommand(cmd);
          c.withCopyToContainer(MountableFile.forClasspathResource("/scripts/rtl"), "/scripts");
          c.withCopyToContainer(MountableFile.forHostPath(outputPath.toString()), "/rtl");
          return c;
        },
        c -> {
          for (var mapping : resultMappings) {
            try {
              Files.createDirectories(new File(mapping.right()).getParentFile().toPath());
            } catch (IOException e) {
              Assertions.fail(e);
            }
            c.copyFileFromContainer(mapping.left(), mapping.right());
          }
        }
    );
  }

  protected String getTopModuleName(String specPath, Map<String, String> macroOverrides) {

    final var spec = runAndGetViamSpecification(specPath, macroOverrides);

    return RtlConfigurationPass.getTopModuleName(spec);
  }

  protected String getDecodeStageName(String specPath, Map<String, String> macroOverrides,
                                      RtlConfiguration config) {

    final var spec = runAndGetViamSpecification(specPath, macroOverrides);

    final var passManager = new PassManager();
    try {
      passManager.add(PassOrders.rtl(config));
      passManager.run(spec);
    } catch (DuplicatedPassKeyException | IOException e) {
      Assertions.fail(e);
    }

    final var mia = spec.mia().orElse(null);
    Assertions.assertNotNull(mia);

    final Stage decodeStage = mia.stages().stream()
        .filter(s -> s.behavior().getNodes(RtlDecodeTreeNode.class).findAny().isPresent())
        .findAny().orElse(null);

    Assertions.assertNotNull(decodeStage);

    return decodeStage.simpleName();
  }
}
