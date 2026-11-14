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

package vadl.iss;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The docker runtime used for ISS tests and benchmarks.
 *
 * <p>We build the generated QEMU during the test docker image build process
 * which has the qemu-test-base image as base image.
 * This image includes dependencies for all test targets and is quite big.
 *
 * <p>We are the buildkit container which allows us to easily attach the C-cache to drastically
 * reduce the build time. However, this also means that after the build finished, all generated
 * artifacts including the entire qemu-test-base image is exported into the docker image format.
 * This takes unreasonably long and produces test images of more than 5 GB.
 *
 * <p>To solve this problem, we split the build image into a multi-stage docker build process,
 * where the final <code>runtime</code> stage only contains the required dependencies,
 * used by the test and benchmark execution. This class defines the runtime stage and its
 * dependencies that are copied from the <code>iss-test-base</code> image.
 */
public enum IssDockerRuntime {
  NONE(List.of()),
  RISCV(List.of("riscv")),
  AARCH32(List.of("aarch32-linux", "aarch32-elf")),
  AARCH64(List.of("aarch64-linux", "aarch64-elf")),
  PPC64(List.of("ppc64"));

  private final List<String> toolchains;

  IssDockerRuntime(List<String> toolchains) {
    this.toolchains = toolchains;
  }

  String dockerfileStage() {
    var copies = toolchains.stream()
        .map(name -> "COPY --from=build-base /opt/" + name + " /opt/" + name + "\n")
        .collect(Collectors.joining());
    var toolchainPath = toolchains.stream()
        .map(name -> "/opt/" + name + "/bin:")
        .collect(Collectors.joining());
    return """
        FROM ubuntu:22.04 AS runtime
        RUN apt-get update && apt-get install -y --no-install-recommends \\
            python3 python3-pip make \\
            libglib2.0-0 libfdt1 libpixman-1-0 zlib1g libstdc++6 libgcc-s1 \\
            libgmp10 libmpfr6 libmpc3 libexpat1 libzstd1 libtinfo6 libncursesw6 libssl3 \\
            && rm -rf /var/lib/apt/lists/*
        RUN pip3 install --no-cache-dir pyyaml qemu.qmp tomli pandas
        """ + copies + "ENV PATH=/opt/qemu/bin:/opt/cosim:" + toolchainPath + "$PATH\n";
  }
}
