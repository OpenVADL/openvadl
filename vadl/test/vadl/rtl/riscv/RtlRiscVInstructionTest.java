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

package vadl.rtl.riscv;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.configuration.DecoderOptions;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.RtlConfiguration;
import vadl.utils.Pair;

class RtlRiscVInstructionTest extends RtlDockerTest {

  /**
   * RTL RISC-V instruction test variants.
   *
   * @return test arguments
   */
  static Stream<Arguments> instructionTestSource() {
    return Stream.of(
        Pair.of("sys/risc-v/mia/rv_3stage.vadl", "RV32I"),
        Pair.of("sys/risc-v/mia/rv_3stage.vadl", "RV32IM"),
        Pair.of("sys/risc-v/mia/rv_3stage.vadl", "RV64I"),
        Pair.of("sys/risc-v/mia/rv_3stage.vadl", "RV64IM"),
        Pair.of("sys/risc-v/mia/rv_5stage.vadl", "RV32I"),
        Pair.of("sys/risc-v/mia/rv_5stage.vadl", "RV32IM"),
        Pair.of("sys/risc-v/mia/rv_5stage.vadl", "RV64I"),
        Pair.of("sys/risc-v/mia/rv_5stage.vadl", "RV64IM")
    ).map(args -> {

      var generalConfig =
          new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE);
      var config = new RtlConfiguration(generalConfig);
      config.setResetVector("reset_vector");

      var decoderOptions = new DecoderOptions();
      decoderOptions.setGenerator(DecoderOptions.Generator.REGULAR);
      config.setDecoderOptions(decoderOptions);

      return Arguments.of(args.left(), args.right(), config);
    });
  }

  @ParameterizedTest
  @MethodSource("instructionTestSource")
  void instructionTest(String specPath, String isa, RtlConfiguration config) {

    runContainerWithSpec(
        specPath,
        Map.of("Isa", isa),
        config,
        Collections.emptyList(),
        "/scripts/test.sh"
    );
  }

}
