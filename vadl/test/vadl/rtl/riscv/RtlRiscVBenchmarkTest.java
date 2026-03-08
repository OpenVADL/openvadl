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

import static vadl.configuration.DecoderOptions.Generator.IRREGULAR;
import static vadl.configuration.DecoderOptions.Generator.REGULAR;
import static vadl.configuration.DecoderOptions.Generator.RTL_TABLE;

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
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.configuration.DecoderOptions;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.RtlConfiguration;
import vadl.pass.PassManager;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.rtl.RtlDockerTest;
import vadl.rtl.ipg.nodes.RtlDecodeTreeNode;
import vadl.utils.Pair;
import vadl.utils.Triple;
import vadl.viam.Stage;

public class RtlRiscVBenchmarkTest extends RtlDockerTest {

  public static final String ENV_RESULT_CSV = "RTL_DEC_BENCHMARK_RESULT_HOST_PATH";

  /**
   * Decode benchmark variants.
   *
   * @return the test arguments
   */
  static Stream<Arguments> decodeBenchmarkTestSource() {
    return Stream.of(
        Triple.of("sys/risc-v/mia/rv_3stage.vadl", "rv32i-3stage-rtl-table", RTL_TABLE),
        Triple.of("sys/risc-v/mia/rv_3stage.vadl", "rv32i-3stage-vdt-regular", REGULAR),
        Triple.of("sys/risc-v/mia/rv_3stage.vadl", "rv32i-3stage-vdt-irregular", IRREGULAR),
        Triple.of("sys/risc-v/mia/rv_5stage.vadl", "rv32i-5stage-rtl-table", RTL_TABLE),
        Triple.of("sys/risc-v/mia/rv_5stage.vadl", "rv32i-5stage-vdt-regular", REGULAR),
        Triple.of("sys/risc-v/mia/rv_5stage.vadl", "rv32i-5stage-vdt-irregular", IRREGULAR)
    ).map(args -> {

      var generalConfig =
          new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE);
      var config = new RtlConfiguration(generalConfig);
      config.setResetVector("reset_vector");

      var decoderOptions = new DecoderOptions();
      decoderOptions.setGenerator(args.right());
      config.setDecoderOptions(decoderOptions);

      return Arguments.of(args.left(), args.middle(), config);
    });
  }

  /**
   * Execute the benchmark script and append the result CSV with the result metrics.
   *
   * @param spec   The VADL specification to run.
   * @param tag    The tag of the version that is tested.
   * @param config The run config.
   */
  @ParameterizedTest
  @Tag("BenchmarkTest")
  @MethodSource("decodeBenchmarkTestSource")
  void decodeBenchmark(String spec, String tag, RtlConfiguration config) {

    // GIVEN
    var yosysLog = new File("build/test-output/bench/synth_decode_" + tag + ".log");
    var openStaLog = new File("build/test-output/bench/time_decode_" + tag + ".log");

    var resultMappings = List.of(
        Pair.of("/rtl/build/synth_decode.log", yosysLog.toString()),
        Pair.of("/rtl/build/time_decode.log", openStaLog.toString())
    );

    var decodeModule = getDecodeStageName(spec, config);

    // WHEN
    runBenchmarkWithSpec(spec, config, resultMappings,
        "/scripts/bench/bench_decode.sh", decodeModule);

    // THEN

    final BigDecimal chipArea = getMetric(yosysLog,
        Pattern.compile(
            "Chip area for module '\\\\" + decodeModule + "': (?<chiparea>\\d+\\.\\d+)"),
        m -> new BigDecimal(m.group("chiparea")));
    Assertions.assertNotNull(chipArea);

    final BigDecimal dataArrivalTime = getMetric(openStaLog,
        Pattern.compile("(?<dataArrivalTime>\\d+\\.\\d+)\\s*data arrival time"),
        m -> new BigDecimal(m.group("dataArrivalTime")));
    Assertions.assertNotNull(dataArrivalTime);

    final String envResultPath = System.getenv(ENV_RESULT_CSV);
    final File result =
        new File(envResultPath != null ? envResultPath : "build/test-output/bench/result.csv");
    final boolean withHeader = !result.exists();

    try (PrintWriter writer = new PrintWriter(
        new FileWriter(result, StandardCharsets.UTF_8, true))) {

      if (withHeader) {
        writer.println("spec,tag,chip area,data arrival time");
      }

      writer.print(spec + ",");
      writer.print(tag + ",");
      writer.print(chipArea.toPlainString() + ",");
      writer.println(dataArrivalTime.toPlainString());

      writer.flush();
    } catch (IOException e) {
      Assertions.fail(e);
    }
  }

  private void runBenchmarkWithSpec(String spec, RtlConfiguration config,
                                    List<Pair<String, String>> resultMappings,
                                    String... cmd) {
    // Create image from the rtl output & scripts
    final var image = generateRtlImage(spec, config);
    // Run the container with the benchmarking script
    runContainerWithInAndOutput(image, List.of(), resultMappings, cmd);
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

  private String getDecodeStageName(String specPath, RtlConfiguration config) {

    final var spec = runAndGetViamSpecification(specPath);

    final var passManager = new PassManager();
    try {
      passManager.add(PassOrders.rtl(config));
      passManager.run(spec);
    } catch (DuplicatedPassKeyException | IOException e) {
      Assertions.fail(e);
    }

    final var mia = spec.mia().orElse(null);
    Assertions.assertNotNull(mia);

    final Stage decodeStage = mia.stages().stream()
        .filter(s -> s.behavior().getNodes(RtlDecodeTreeNode.class).findAny().isPresent())
        .findAny().orElse(null);

    Assertions.assertNotNull(decodeStage);

    return decodeStage.simpleName();
  }

}
