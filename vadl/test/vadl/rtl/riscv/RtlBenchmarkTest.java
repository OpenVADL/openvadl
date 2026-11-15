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
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import vadl.configuration.RtlConfiguration;
import vadl.utils.Pair;

public abstract class RtlBenchmarkTest extends RtlDockerTest {

  public static final int MAX_CLOCK_NS = 25;

  private static final String PLACEMENT_AREA_PATTERN =
      "Final placement area: (?<area>\\d+(\\.\\d+)?) \\((?<delta>[+\\-]\\d+(\\.\\d+))%\\)";

  /**
   * Get environment variable that determines where to save the result CSV file.
   *
   * @return environment variable name
   */
  protected abstract String getResultCsvEnv();

  /**
   * Get fallback result CSV filename. This file is written, if the environment variable
   * {@link #getResultCsvEnv()} is not set.
   *
   *  @return fallback filename
   */
  protected abstract String getResultCsvFallback();

  /**
   * Execute the benchmark script and append the result CSV with the result metrics.
   *
   * @param specPath   The VADL specification to run.
   * @param tag    The tag of the version that is tested.
   * @param config The run config.
   */
  void benchmark(String specPath, String tag, RtlConfiguration config, String isa) {

    var macroOverrides = Map.of("Isa", isa);

    var coreLog = new File("build/test-output/bench/core_iter_" + tag + ".log");
    var decodeLog = new File("build/test-output/bench/decode_" + tag + ".log");

    var topModule = getTopModuleName(specPath, macroOverrides);
    var decodeModule = getDecodeStageName(specPath, macroOverrides, config);


    // run synthesis and placement for the whole core
    runRtlContainerWithSpec(specPath, macroOverrides, config,
        List.of(Pair.of("/rtl/core_iter.log", coreLog.toString())),
        "/bin/bash", "-c",
        "/scripts/bench/bench_core_iter.sh " + topModule + " 0 " + MAX_CLOCK_NS
            + "  | tee /rtl/core_iter.log");

    var areaAndDelta = getMetric(coreLog, Pattern.compile(PLACEMENT_AREA_PATTERN),
        m -> Pair.of(
            new BigDecimal(m.group("area")),
            new BigDecimal(m.group("delta"))
        ));
    Assertions.assertNotNull(areaAndDelta);

    final BigDecimal clock = getMetric(coreLog,
        Pattern.compile("best clock (?<clock>\\d+(\\.\\d+)?) ns"),
        m -> new BigDecimal(m.group("clock")));
    Assertions.assertNotNull(clock);


    // synthesize and place the decode stage with the core's clock period
    runRtlContainerWithGenerated(config,
        List.of(Pair.of("/rtl/decode.log", decodeLog.toString())),
        "/bin/bash", "-c",
        "/scripts/bench/bench_core.sh " + decodeModule + " " + clock.toPlainString()
            + "  | tee /rtl/decode.log");

    var decodeArea = getMetric(decodeLog, Pattern.compile(PLACEMENT_AREA_PATTERN),
        m -> new BigDecimal(m.group("area")));
    Assertions.assertNotNull(decodeArea);

    // write results
    final String envResultPath = System.getenv(getResultCsvEnv());
    final File result =
        new File(envResultPath != null ? envResultPath : getResultCsvFallback());
    final boolean withHeader = !result.exists();

    try (PrintWriter writer = new PrintWriter(
        new FileWriter(result, StandardCharsets.UTF_8, true))) {

      if (withHeader) {
        println(writer, "spec,tag,decode area,chip area,clock period,timing delta");
      }

      print(writer, specPath + ",");
      print(writer, tag + ",");
      print(writer, decodeArea.toPlainString() + ",");
      print(writer, areaAndDelta.left().toPlainString() + ",");
      print(writer, clock.toPlainString() + ",");
      println(writer, areaAndDelta.right().toPlainString());

      writer.flush();
    } catch (IOException e) {
      Assertions.fail(e);
    }
  }

  private void print(PrintWriter writer, String str) {
    writer.print(str);
    System.out.print(str);
  }

  private void println(PrintWriter writer, String line) {
    writer.println(line);
    System.out.println(line);
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
