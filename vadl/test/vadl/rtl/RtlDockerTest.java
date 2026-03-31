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

package vadl.rtl;

import static vadl.rtl.passes.EmitRtlDevcontainerDockerComposePass.RTL_BASE_IMAGE;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import vadl.DockerExecutionTest;
import vadl.DockerImage;
import vadl.configuration.RtlConfiguration;
import vadl.pipeline.RtlPassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;

public abstract class RtlDockerTest extends DockerExecutionTest {

  private static final ConcurrentHashMap<String, DockerImage> imageCache =
      new ConcurrentHashMap<>();

  protected DockerImage generateRtlImage(String specPath,
                                         RtlConfiguration configuration) {

    final var cacheKey = String.valueOf(Set.of(specPath, configuration).hashCode());
    return RtlDockerTest.imageCache.computeIfAbsent(cacheKey, (k) -> {

      try {
        // Generate RTL core
        setupPassManagerAndRunSpec(specPath, RtlPassOrders.rtl(configuration));

        // Return image with generated files
        return getImage(configuration);
      } catch (IOException | DuplicatedPassKeyException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private DockerImage getImage(RtlConfiguration configuration
  ) {

    // find output dir
    var outputPath = Path.of(configuration.outputPath() + "/rtl").toAbsolutePath();
    if (!outputPath.toFile().exists()) {
      throw new IllegalStateException("RTL output path was not found (not generated?)");
    }

    return new DockerImage()
        .withDockerfileFromBuilder(d -> {
              d.from(RTL_BASE_IMAGE);

              d.workDir("/rtl");

              // Copy files into container
              d.copy("/rtl", "/rtl");
              d.copy("/scripts", "/scripts");

              d.run("chmod +x /scripts/*");

              d.build();
            }
        )
        // Make generated sources available to image builder
        .withFileFromPath("/rtl", outputPath)
        // make scripts available to image builder
        .withFileFromClasspath("/scripts", "/scripts/rtl");
  }

}
