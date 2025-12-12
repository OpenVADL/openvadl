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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class CosimTestUtils {

  private static final Logger log = LoggerFactory.getLogger(CosimTestUtils.class);

  public record TestCase(
      String id,
      boolean debug,
      String asmCore
  ) {
  }

  public record TestConfig(
      String cosimConfig,
      Collection<TestCase> tests
  ) {
  }

  public record TestResult(
      boolean passed,
      List<Diff> diffs,
      List<DiffContext> diffContext
  ) {
    public record Diff(
        String key,
        List<String> values,
        String description
    ) {
    }

    public record DiffContext(
        Integer clientId,
        String clientName
    ) {
    }
  }

  public static void writeTestSuiteConfigYaml(TestConfig config,
                                              File dest)
      throws IOException {
    var testsYaml = config.tests.stream().map(spec -> {
      var specYaml = new LinkedHashMap<String, Object>();
      specYaml.put("id", spec.id);
      specYaml.put("debug", spec.debug);
      specYaml.put("asm_core", spec.asmCore);
      return specYaml;
    }).toList();

    var rootYaml = new LinkedHashMap<String, Object>();
    rootYaml.put("cosim_config", config.cosimConfig);
    rootYaml.put("tests", testsYaml);

    Yaml yaml = new Yaml();
    try (var writer = new FileWriter(dest)) {
      yaml.dump(rootYaml, writer);
    }
  }

  public static TestResult yamlToTestResult(File yamlFile) {
    try {
      var yaml = new Yaml();
      Map<String, Object> root = yaml.load(Files.readString(yamlFile.toPath()));

      boolean passed = (boolean) root.get("passed");

      List<TestResult.Diff> diffs = new ArrayList<>();
      Object diffsObj = root.get("diffs");
      if (diffsObj instanceof List<?> list) {
        for (Object o : list) {
          if (o instanceof Map<?, ?> m) {
            String key = String.valueOf(m.get("key"));
            List<String> values = new ArrayList<>();
            Object vals = m.get("values");
            if (vals instanceof List<?> lv) {
              for (Object v : lv) {
                values.add(String.valueOf(v));
              }
            }
            String description = String.valueOf(m.get("description"));
            diffs.add(new TestResult.Diff(key, values, description));
          }
        }
      }

      List<TestResult.DiffContext> diffContext = new ArrayList<>();
      Object ctxObj = root.get("diff_context");
      if (ctxObj instanceof List<?> ctxList) {
        for (Object o : ctxList) {
          if (o instanceof Map<?, ?> m) {
            Integer clientId = m.get("client_id") instanceof Number n ? n.intValue() : null;
            String clientName =
                m.get("client_name") == null ? null : String.valueOf(m.get("client_name"));
            diffContext.add(new TestResult.DiffContext(clientId, clientName));
          }
        }
      }

      return new TestResult(passed, diffs, diffContext);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
