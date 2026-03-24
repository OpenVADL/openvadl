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
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.DockerExecutionTest;
import vadl.DockerImage;
import vadl.configuration.IssConfiguration;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;

/**
 * The test class to build and run tests on the QEMU ISS.
 * The {@link #generateIssSimulator(String)} methods runs the ISS generation and builds
 * a working QEMU image with the new target.
 * Every target specification is cached and therefore only built for the first test.
 *
 * <p>The class also provides functions to automatically run tests in the container.</p>
 */
public abstract class CosimTest extends DockerExecutionTest {

  // config of qemu test image
  private static final String QEMU_TEST_IMAGE =
      "ghcr.io/openvadl/iss-test-base@sha256:134d41337274a2f54790c13582e14a23a78617bb90554214f8a6f721c5287e85";

  // specification to image cache
  private static final ConcurrentHashMap<String, DockerImage> issImageCache =
      new ConcurrentHashMap<>();

  private static final Logger log = LoggerFactory.getLogger(CosimTest.class);

  @Override
  public IssConfiguration getConfiguration(boolean doDump) {
    return IssConfiguration.from(super.getConfiguration(doDump));
  }

  /**
   * This will run the given specification and produces a working docker image that contains
   * a compiled QEMU ISS from the specification.
   *
   * <p>If this ISS specification was already build by some other test, the image is reused.</p>
   *
   * @param specPath path to VADL specification in testSource
   * @return the image containing the generated QEMU ISS
   */
  protected DockerImage generateIssSimulator(String specPath) {
    var config = IssConfiguration.from(getConfiguration(false));
    return generateSimulator(issImageCache, specPath, config);
  }

  /**
   * This will generate the simulator image if it is not already contained in the provided
   * cache.
   */
  private DockerImage generateSimulator(Map<String, DockerImage> cache,
                                        String specPath,
                                        IssConfiguration configuration) {
    return cache.computeIfAbsent(specPath, (path) -> {
      try {
        // run iss generation
        setupPassManagerAndRunSpec(path, PassOrders.iss(configuration));

        // find iss output path
        var issOutputPath = Path.of(configuration.outputPath() + "/iss").toAbsolutePath();
        if (!issOutputPath.toFile().exists()) {
          throw new IllegalStateException("ISS output path was not found (not generated?)");
        }

        // generate iss image from the output path
        return getIssImage(issOutputPath, configuration);
      } catch (IOException | DuplicatedPassKeyException e) {
        throw new RuntimeException(e);
      }
    });
  }

  protected abstract String getScriptFolder();

  protected abstract String withUpstreamTarget();

  /**
   * This will produce a new image for the given generated iss sources.
   *
   * @param generatedIssSources the path to the generated ISS/QEMU sources.
   * @return a new image that builds the ISS at build time.
   */
  private DockerImage getIssImage(Path generatedIssSources,
                                  IssConfiguration configuration
  ) {

    var targetName = configuration.targetName().toLowerCase();
    var softmmuTarget = targetName + "-softmmu";
    var qemuBin = "qemu-system-" + targetName;
    var refTarget = "," + withUpstreamTarget();

    return new DockerImage()
        .withDockerfile(new StringSubstitutor(Map.of("qemu", QEMU_TEST_IMAGE,
            "soft", softmmuTarget + refTarget,
            "qemu-bin", qemuBin)).replace("""
            FROM ${qemu}
            COPY iss /qemu
            WORKDIR /qemu/build
            LABEL key=VADL_TEST_CONTAINER
            RUN ../configure --cc='sccache gcc' --target-list=${soft}
            RUN --mount=type=cache,target=/root/.cache/sccache sccache --start-server && make -j$(nproc) && sccache -s
            RUN ${qemu-bin} --version
            WORKDIR /work
            
            COPY vadl-cosim /work/vadl-cosim
            WORKDIR /work/vadl-cosim
            ENV RUSTC_WRAPPER "sccache"
            # use --frozen to ensure that cargo does not need to download any new dependencies
            RUN --mount=type=cache,target=/root/.cache/sccache sccache --start-server && cargo build --release -p vadl-cosim-broker --frozen && sccache -s
            
            RUN mkdir -p /opt/cosim && cp ./target/release/vadl-cosim-broker /opt/cosim/
            ENV PATH "/opt/cosim:$PATH"
            
            WORKDIR /work
            
            COPY cosim_configs /cosim_configs
            COPY cosim_scripts /cosim_scripts
            
            CMD python3 /cosim_scripts/main.py test-suite.yaml
            """))
        // make iss sources available to image builder
        .withFileFromPath("iss", generatedIssSources)
        // make cosim scripts and configs available to image builder
        .withFileFromClasspath("/cosim_configs", "/cosim_configs")
        .withFileFromClasspath("/cosim_scripts", "/cosim_scripts/" + getScriptFolder())
        // add vadl-cosim to image builder
        .withFileFromPath("/vadl-cosim", Path.of("..", "vadl-cosim"));
  }

}
