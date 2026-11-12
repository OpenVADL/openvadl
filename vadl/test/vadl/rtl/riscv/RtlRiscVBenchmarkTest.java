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

import static vadl.configuration.DecoderOptions.Generator.IRREGULAR;
import static vadl.configuration.DecoderOptions.Generator.REGULAR;
import static vadl.configuration.DecoderOptions.Generator.RTL_TABLE;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.configuration.DecoderOptions;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.RtlConfiguration;
import vadl.utils.Quadruple;

public class RtlRiscVBenchmarkTest extends RtlBenchmarkTest {

  @Override
  protected String getResultCsvEnv() {
    return "RTL_BENCHMARK_RESULT_HOST_PATH";
  }

  @Override
  protected String getResultCsvFallback() {
    return "build/test-output/bench/rtl-result.csv";
  }

  /**
   * RTL RISC-V benchmark variants.
   *
   * @return test arguments
   */
  static Stream<Arguments> benchmarkTestSource() {
    return Stream.of(
        Quadruple.of("sys/risc-v/mia/rv_3stage.vadl", "rv32i-3stage-rtl-table", RTL_TABLE, "RV32I"),
        Quadruple.of("sys/risc-v/mia/rv_3stage.vadl", "rv32i-3stage-vdt-regular", REGULAR, "RV32I"),
        Quadruple.of("sys/risc-v/mia/rv_3stage.vadl", "rv32i-3stage-vdt-irregular", IRREGULAR, "RV32I"),
        Quadruple.of("sys/risc-v/mia/rv_5stage.vadl", "rv32i-5stage-rtl-table", RTL_TABLE, "RV32I"),
        Quadruple.of("sys/risc-v/mia/rv_5stage.vadl", "rv32i-5stage-vdt-regular", REGULAR, "RV32I"),
        Quadruple.of("sys/risc-v/mia/rv_5stage.vadl", "rv32i-5stage-vdt-irregular", IRREGULAR, "RV32I")
    ).map(args -> {

      var generalConfig =
          new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE);
      var config = new RtlConfiguration(generalConfig);
      config.setResetVector("reset_vector");

      var decoderOptions = new DecoderOptions();
      decoderOptions.setGenerator(args.third());
      config.setDecoderOptions(decoderOptions);

      return Arguments.of(args.first(), args.second(), config, args.fourth());
    });
  }

  @Override
  @ParameterizedTest
  @Tag("BenchmarkTest")
  @MethodSource("benchmarkTestSource")
  void benchmark(String specPath, String tag, RtlConfiguration config, String isa) {
    super.benchmark(specPath, tag, config, isa);
  }

}
