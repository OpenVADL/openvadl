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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import vadl.configuration.DecoderOptions;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.RtlConfiguration;
import vadl.rtl.RtlDockerTest;
import vadl.utils.Pair;

public class RV32SynthesisBenchmarkTest extends RtlDockerTest {

  public static final String ENV_RESULT_CSV = "RTL_DEC_BENCHMARK_RESULT_HOST_PATH";

  @Test
  @Order(0)
  @Tag("BenchmarkTest")
  void rv32imDecodeTable() {

    // GIVEN
    var generalConfig =
        new GeneralConfiguration(Path.of("build/test-output"), false);

    var config = new RtlConfiguration(generalConfig);
    config.setDummyMia(RtlConfiguration.DummyMia.five);

    var decoderOptions = new DecoderOptions();
    decoderOptions.setGenerator(DecoderOptions.Generator.RTL_TABLE);
    config.setDecoderOptions(decoderOptions);

    // WHEN / THEN
    runBenchmark("sys/risc-v/rv32im.vadl", "rv32im-rtl-table", config);
  }

  @Test
  @Order(1)
  @Tag("BenchmarkTest")
  void rv32imDecodeRegular() {

    // GIVEN
    var generalConfig =
        new GeneralConfiguration(Path.of("build/test-output"), false);

    var config = new RtlConfiguration(generalConfig);
    config.setDummyMia(RtlConfiguration.DummyMia.five);

    var decoderOptions = new DecoderOptions();
    decoderOptions.setGenerator(DecoderOptions.Generator.REGULAR);
    config.setDecoderOptions(decoderOptions);

    // WHEN / THEN
    runBenchmark("sys/risc-v/rv32im.vadl", "rv32im-vdt-regular", config);
  }

  @Test
  @Order(2)
  @Tag("BenchmarkTest")
  void rv32imDecodeIrregular() {

    // GIVEN
    var generalConfig =
        new GeneralConfiguration(Path.of("build/test-output"), false);

    var config = new RtlConfiguration(generalConfig);
    config.setDummyMia(RtlConfiguration.DummyMia.five);

    var decoderOptions = new DecoderOptions();
    decoderOptions.setGenerator(DecoderOptions.Generator.IRREGULAR);
    config.setDecoderOptions(decoderOptions);

    // WHEN / THEN
    runBenchmark("sys/risc-v/rv32im.vadl", "rv32im-vdt-irregular", config);
  }

  /**
   * Execute the benchmark script and append the result CSV with the result metrics.
   *
   * @param spec   The VADL specification to run.
   * @param tag    The tag of the version that is tested.
   * @param config The run config.
   */
  private void runBenchmark(String spec, String tag, RtlConfiguration config) {

    // GIVEN
    var yosysLog = new File("build/test-output/bench/synth_decode_" + tag + ".log");
    var openStaLog = new File("build/test-output/bench/time_decode_" + tag + ".log");

    var resultMappings = List.of(
        Pair.of("/rtl/build/synth_decode.log", yosysLog.toString()),
        Pair.of("/rtl/build/time_decode.log", openStaLog.toString())
    );

    // WHEN
    runBenchmarkWithSpec("sys/risc-v/rv32im.vadl", config, resultMappings);

    // THEN

    final BigDecimal chipArea = getMetric(yosysLog,
        Pattern.compile("Chip area for module '\\\\DECODE': (?<chiparea>\\d+\\.\\d+)"),
        m -> new BigDecimal(m.group("chiparea")));
    Assertions.assertNotNull(chipArea);

    final BigDecimal timingSlack = getMetric(openStaLog,
        Pattern.compile("(?<slack>\\d+\\.\\d+)\\s*slack \\(MET\\)"),
        m -> new BigDecimal(m.group("slack")));
    Assertions.assertNotNull(timingSlack);

    final String envResultPath = System.getenv(ENV_RESULT_CSV);
    final File result =
        new File(envResultPath != null ? envResultPath : "build/test-output/bench/result.csv");
    final boolean withHeader = !result.exists();

    try (PrintWriter writer = new PrintWriter(
        new FileWriter(result, StandardCharsets.UTF_8, true))) {

      if (withHeader) {
        writer.println("spec,tag,chip area,timing slack");
      }

      writer.print(spec + ",");
      writer.print(tag + ",");
      writer.print(chipArea.toPlainString() + ",");
      writer.println(timingSlack.toPlainString());

      writer.flush();
    } catch (IOException e) {
      Assertions.fail(e);
    }
  }

  private void runBenchmarkWithSpec(String spec, RtlConfiguration config,
                                    List<Pair<String, String>> resultMappings) {
    // Create image from the rtl output & scripts
    final var image = generateRtlImage(spec, config);
    // Run the container with the benchmarking script
    runContainerWithInAndOutput(image, List.of(), resultMappings, "/scripts/bench/bench.sh");
  }

  private <T> T getMetric(File input, Pattern pattern, Function<MatchResult, T> extractor) {

    try (Scanner s = new Scanner(input)) {
      s.findWithinHorizon(pattern, 0);
      return extractor.apply(s.match());
    } catch (FileNotFoundException e) {
      Assertions.fail(e);
    }
    return null;
  }

}
