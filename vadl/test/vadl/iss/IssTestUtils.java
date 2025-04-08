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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class IssTestUtils {

  private static final Logger log = LoggerFactory.getLogger(IssTestUtils.class);

  protected record TestCase(
      String id,
      String asmCore
  ) {
  }

  protected record TestConfig(
      // { path: <str>, args: <str> }
      Map<String, String> sim,
      // { path: <str>, args: <str> }
      Map<String, String> ref,
      // { path: <str>, args: <str> }
      Map<String, String> compiler,
      String statePlugin,
      Collection<TestCase> tests,
      Map<String, String> gdbRegMap
  ) {
  }

  protected record TestResult(
      String id,
      TestResult.Status status,
      List<TestResult.Stage> completedStages,
      List<TestResult.RegTestResult> regTests,
      Map<String, List<String>> simLogs,
      Map<String, List<String>> refLogs,
      List<String> errors,
      String duration
  ) {

    protected enum Status {
      PASS,
      FAIL
    }

    protected enum Stage {
      COMPILE,
      LINK,
      RUN,
      RUN_REF,
      COMPARE
    }

    protected record RegTestResult(
        String reg,
        String expected,
        String actual
    ) {
    }
  }

  /**
   * Writes the test suite configuration YAML file.
   */
  protected static void writeTestSuiteConfigYaml(TestConfig config,
                                                 File dest)
      throws IOException {

    var testsYaml = config.tests.stream().map(spec -> {
      var specYaml = new LinkedHashMap<String, Object>();
      specYaml.put("id", spec.id);
      specYaml.put("asm_core", spec.asmCore);
      return specYaml;
    }).toList();

    var conigYaml = new LinkedHashMap<String, Object>();
    conigYaml.put("sim", config.sim);
    conigYaml.put("ref", config.ref);
    conigYaml.put("compiler", config.compiler);
    conigYaml.put("stateplugin", config.statePlugin);
    conigYaml.put("tests", testsYaml);
    conigYaml.put("gdbregmap", config.gdbRegMap());

    Yaml yaml = new Yaml();
    try (var writer = new FileWriter(dest)) {
      yaml.dump(conigYaml, writer);
    }
  }

  /**
   * Converts a YAML file to a TestResult object.
   *
   * @param yamlFile The YAML file to convert.
   * @return The converted TestResult object.
   * @throws IOException if an I/O error occurs.
   */
  protected static TestResult yamlToTestResult(File yamlFile) {
    try (var reader = new FileInputStream(yamlFile)) {
      Yaml yaml = new Yaml();

      // load all results
      Map<String, Object> result = yaml.load(reader);

      try {
        String id = result.get("id").toString();
        // Assuming the YAML structure matches the fields in TestResult
        TestResult.Status status =
            TestResult.Status.valueOf((String) result.get("status"));
        List<TestResult.Stage> completedStages =
            ((List<String>) result.get("completedStages")).stream()
                .map(e -> TestResult.Stage.valueOf(e))
                .toList();
        List<String> errors = (List<String>) result.get("errors");
        String duration = (String) result.get("duration");
        List<TestResult.RegTestResult> regTests =
            ((Map<String, Object>) result.get("regTests")).entrySet()
                .stream().map(e -> {
                  var val = (Map<String, String>) e.getValue();
                  return new TestResult.RegTestResult(e.getKey(),
                      Objects.toString(val.get("exp")),
                      Objects.toString(val.get("act")));
                }).toList();

        Map<String, List<String>> simLogs = (Map<String, List<String>>) result.get("simLogs");
        Map<String, List<String>> refLogs = (Map<String, List<String>>) result.get("refLogs");

        return new TestResult(id, status, completedStages, regTests, simLogs, refLogs, errors,
            duration);

      } catch (Exception e) {
        log.error("Failed to parse file, result was\n {}", result);
        throw new RuntimeException(e);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
