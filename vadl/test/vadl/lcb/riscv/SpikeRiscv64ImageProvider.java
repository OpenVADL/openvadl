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

import java.nio.file.Paths;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.DockerExecutionTest;

/**
 * A singleton implementation to keep the reference to a {@link ImageFromDockerfile} to avoid
 * recompilation between {@link LlvmRiscvAssemblyTest} and {@link SpikeRiscvSimulationTest}.
 */
public class SpikeRiscv64ImageProvider {
  private static ImageFromDockerfile image;

  /**
   * Create an {@link ImageFromDockerfile} or return an already existing image.
   *
   * @param redisCache          is the cache for building LLVM.
   * @param pathDockerFile      is the path to the dockerfile which should be built.
   * @param target              is the name of the processor.
   * @param upstreamBuildTarget is the name of LLVM backend to compile an upstream compiler.
   * @param upstreamClangTarget is the name for the LLVM clang option to invoke the upstream
   *                            compiler.
   * @param spikeTarget         is the ISA for spike to run the executable.
   * @param doDebug             if the flag is {@code true} then the image will not be deleted.
   */
  public static ImageFromDockerfile image(DockerExecutionTest.RedisCache redisCache,
                                          String pathDockerFile,
                                          String target,
                                          String upstreamBuildTarget,
                                          String upstreamClangTarget,
                                          String spikeTarget,
                                          boolean doDebug) {
    if (image == null) {
      var img = redisCache.setupEnv(new ImageFromDockerfile("tc_spike_riscv64", !doDebug)
          .withDockerfile(Paths.get(pathDockerFile))
          .withBuildArg("TARGET", target)
          .withBuildArg("UPSTREAM_BUILD_TARGET", upstreamBuildTarget)
          .withBuildArg("UPSTREAM_CLANG_TARGET", upstreamClangTarget)
          .withBuildArg("SPIKE_TARGET", spikeTarget));
      image = img;
      return img;
    } else {
      return image;
    }
  }
}
