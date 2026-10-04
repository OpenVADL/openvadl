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

import static vadl.rtl.passes.EmitRtlDevcontainerDockerComposePass.RTL_BASE_IMAGE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.testcontainers.utility.MountableFile;
import vadl.DockerExecutionTest;
import vadl.configuration.RtlConfiguration;
import vadl.pass.PassManager;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.rtl.ipg.nodes.RtlDecodeTreeNode;
import vadl.rtl.passes.RtlConfigurationPass;
import vadl.utils.Pair;
import vadl.viam.Stage;

public abstract class RtlBenchmarkTest extends DockerExecutionTest {

  public static final int MAX_CLOCK_NS = 25;

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
   * @param spec   The VADL specification to run.
   * @param tag    The tag of the version that is tested.
   * @param config The run config.
   */
  void benchmark(String spec, String tag, RtlConfiguration config, String isa) {

    var macroOverrides = Map.of("Isa", isa);

    var log = new File("build/test-output/bench/core_iter_" + tag + ".log");

    var resultMappings = List.of(
        Pair.of("/rtl/core_iter.log", log.toString())
    );

    var topModule = getTopModuleName(spec, macroOverrides);
    var decodeModule = getDecodeStageName(spec, macroOverrides, config);

    runBenchmarkWithSpec(spec, macroOverrides, config, resultMappings,
        "/bin/bash", "-c",
        "/scripts/bench/bench_core_iter.sh " + topModule + " 0 " + MAX_CLOCK_NS
            + "  | tee /rtl/core_iter.log");

    var decodeArea = getMetric(log,
        Pattern.compile(
            "Chip area for module '\\\\" + decodeModule + "': (?<area>\\d+(\\.\\d+)?)"),
        m -> new BigDecimal(m.group("area")));
    Assertions.assertNotNull(decodeArea);

    var areaAndDelta = getMetric(log,
        Pattern.compile(
            "Final placement area: (?<area>\\d+(\\.\\d+)?) \\(\\+(?<delta>\\d+(\\.\\d+))%\\)"),
        m -> Pair.of(
            new BigDecimal(m.group("area")),
            new BigDecimal(m.group("delta"))
        ));
    Assertions.assertNotNull(areaAndDelta);

    final BigDecimal clock = getMetric(log,
        Pattern.compile("best clock (?<clock>\\d+(\\.\\d+)?) ns"),
        m -> new BigDecimal(m.group("clock")));
    Assertions.assertNotNull(clock);

    final String envResultPath = System.getenv(getResultCsvEnv());
    final File result =
        new File(envResultPath != null ? envResultPath : getResultCsvFallback());
    final boolean withHeader = !result.exists();

    try (PrintWriter writer = new PrintWriter(
        new FileWriter(result, StandardCharsets.UTF_8, true))) {

      if (withHeader) {
        println(writer, "spec,tag,decode area,chip area,clock period,timing delta");
      }

      print(writer, spec + ",");
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

  private void runBenchmarkWithSpec(String spec, Map<String, String> macroOverrides,
                                    RtlConfiguration config,
                                    List<Pair<String, String>> resultMappings,
                                    String... cmd) {
    // Generate RTL core
    try {
      setupPassManagerAndRunSpec(spec, PassOrders.rtl(config), macroOverrides);
    } catch (IOException | DuplicatedPassKeyException e) {
      throw new RuntimeException(e);
    }

    // Input files
    var outputPath = Path.of(config.outputPath() + "/rtl").toAbsolutePath();
    if (!outputPath.toFile().exists()) {
      throw new IllegalStateException("RTL output path was not found (not generated?)");
    }

    runContainer(
        RTL_BASE_IMAGE,
        c -> {
          c.setCommand(cmd);
          c.withCopyToContainer(MountableFile.forClasspathResource("/scripts/rtl"), "/scripts");
          c.withCopyToContainer(MountableFile.forHostPath(outputPath.toString()), "/rtl");
          return c;
        },
        c -> {
          for (var mapping : resultMappings) {
            try {
              Files.createDirectories(new File(mapping.right()).getParentFile().toPath());
            } catch (IOException e) {
              Assertions.fail(e);
            }
            c.copyFileFromContainer(mapping.left(), mapping.right());
          }
        }
    );
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

  private String getTopModuleName(String specPath, Map<String, String> macroOverrides) {

    final var spec = runAndGetViamSpecification(specPath, macroOverrides);

    return RtlConfigurationPass.getTopModuleName(spec);
  }

  private String getDecodeStageName(String specPath, Map<String, String> macroOverrides,
                                    RtlConfiguration config) {

    final var spec = runAndGetViamSpecification(specPath, macroOverrides);

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
