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
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
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
public abstract class QemuIssTest extends DockerExecutionTest {

  // config of qemu test image
  private static final String QEMU_TEST_IMAGE =
      "ghcr.io/openvadl/qemu-base@sha256:a5a09dbc89913461f38eb206ceb7dbb1d3b0969355c7cd916cf50159dcbe6900";

  // specification to image cache
  // we must separate CAS and ISS, otherwise the CAS test would use the ISS image
  private static final ConcurrentHashMap<String, ImageFromDockerfile> issImageCache =
      new ConcurrentHashMap<>();
  private static final ConcurrentHashMap<String, ImageFromDockerfile> casImageCache =
      new ConcurrentHashMap<>();

  private static final Logger log = LoggerFactory.getLogger(QemuIssTest.class);

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
        return getIssImage(issOutputPath, configuration, "riscv64-softmmu", "riscv32-softmmu");
      } catch (IOException | DuplicatedPassKeyException e) {
        throw new RuntimeException(e);
      }
    });
  }


  /**
   * This will produce a new image for the given generated iss sources.
   *
   * @param generatedIssSources the path to the generated ISS/QEMU sources.
   * @param referenceTargets    The reference targets that should also be build
   *                            (e.g. riscv64-softmmu)
   * @return a new image that builds the ISS at build time.
   */
  private ImageFromDockerfile getIssImage(Path generatedIssSources,
                                          IssConfiguration configuration,
                                          String... referenceTargets
  ) {

    // get redis cache for faster compilation using sccache
    var redisCache = getRunningRedisCache();

    var targetName = configuration.targetName().toLowerCase();
    var softmmuTarget = targetName + "-softmmu";
    var qemuBin = "qemu-system-" + targetName;
    var refTargetString = Arrays.stream(referenceTargets).collect(Collectors.joining(","));
    var refTarget = refTargetString.isEmpty() ? "" : "," + refTargetString;

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

              d.copy("/scripts", "/scripts");
              d.run("ls /scripts");
              d.cmd("python3 /scripts/main.py test-suite.yaml");

              d.build();
            }
        )
        // make iss sources available to image builder
        .withFileFromPath("iss", generatedIssSources)
        // make iss_qemu scripts available to image builder
        .withFileFromClasspath("/scripts", "/scripts/iss_qemu");

    // as we have to use the same network as the redis cache, we have to build it there
    return redisCache.setupEnv(dockerImage);
  }

}
