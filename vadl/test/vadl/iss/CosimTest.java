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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.DockerExecutionTest;
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
      "ghcr.io/openvadl/iss-test-base@sha256:e70f997ba639324b1e43ac08fee9460b10e321dfec3da1a6e710eae419acf2e1";

  // specification to image cache
  private static final ConcurrentHashMap<String, ImageFromDockerfile> issImageCache =
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
  protected ImageFromDockerfile generateIssSimulator(String specPath) {
    var config = IssConfiguration.from(getConfiguration(false));
    return generateSimulator(issImageCache, specPath, config);
  }

  /**
   * This will generate the simulator image if it is not already contained in the provided
   * cache.
   */
  private ImageFromDockerfile generateSimulator(Map<String, ImageFromDockerfile> cache,
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
  private ImageFromDockerfile getIssImage(Path generatedIssSources,
                                          IssConfiguration configuration
  ) {

    // get redis cache for faster compilation using sccache
    var redisCache = getRunningRedisCache();

    var targetName = configuration.targetName().toLowerCase();
    var softmmuTarget = targetName + "-softmmu";
    var qemuBin = "qemu-system-" + targetName;
    var refTarget = "," + withUpstreamTarget();

    var dockerImage = new ImageFromDockerfile()
        .withDockerfileFromBuilder(d -> {
              d
                  .from(QEMU_TEST_IMAGE)
                  .copy("iss", "/qemu");

              // use redis cache for building (sccache allows remote caching)
              var cc = "sccache gcc";

              d.workDir("/qemu/build");
              // configure qemu with the new target from the specification
              d.run("../configure --cc='" + cc + "' --target-list=" + softmmuTarget + refTarget);
              // setup redis cache endpoint environment variablef
              redisCache.setupEnv(d);
              // build qemu with all cpu cores and print if cache was used.
              // the sccache --start-server is required,
              // otherwise we get a deadlock after the last make step.
              // see https://github.com/mozilla/sccache/issues/2145
              d.run("sccache --start-server && make -j$(nproc) && sccache -s");
              // validate existence of generated qemu iss
              d.run(qemuBin + " --version");

              d.workDir("/work");

              // build the cosim broker
              d.copy("/vadl-cosim", "/work/vadl-cosim");
              d.workDir("/work/vadl-cosim");
              d.env("RUSTC_WRAPPER", "sccache");
              // use --frozen to ensure that cargo does not need to download any new dependencies
              d.run("sccache --start-server && cargo build --release -p vadl-cosim-broker --frozen && sccache -s");

              // add cosim broker to path
              d.run("mkdir -p /opt/cosim && cp ./target/release/vadl-cosim-broker /opt/cosim/");
              d.env("PATH", "/opt/cosim:$PATH");

              d.workDir("/work");

              d.copy("/cosim_configs", "/cosim_configs");
              d.copy("/cosim_scripts", "/cosim_scripts");

              d.cmd("python3 /cosim_scripts/main.py test-suite.yaml");

              d.build();
            }
        )
        // make iss sources available to image builder
        .withFileFromPath("iss", generatedIssSources)
        // make cosim scripts and configs available to image builder
        .withFileFromClasspath("/cosim_configs", "/cosim_configs")
        .withFileFromClasspath("/cosim_scripts", "/cosim_scripts/" + getScriptFolder())
        // add vadl-cosim to image builder
        .withFileFromPath("/vadl-cosim", Path.of("..", "vadl-cosim"));

    // as we have to use the same network as the redis cache, we have to build it there
    return redisCache.setupEnv(dockerImage);
  }

}
