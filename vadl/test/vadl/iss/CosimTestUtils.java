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
import java.util.Collection;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class CosimTestUtils {

  private static final Logger log = LoggerFactory.getLogger(CosimTestUtils.class);

  public record TestCase(
      String id,
      String asmCore
  ) {
  }

  public record TestConfig(
      String cosimConfig,
      String compiler,
      Collection<TestCase> tests
  ) {
  }

  public static void writeTestSuiteConfigYaml(TestConfig config,
                                              File dest)
      throws IOException {
    var testsYaml = config.tests.stream().map(spec -> {
      var specYaml = new LinkedHashMap<String, Object>();
      specYaml.put("id", spec.id);
      specYaml.put("asm_core", spec.asmCore);
      return specYaml;
    }).toList();

    var rootYaml = new LinkedHashMap<String, Object>();
    rootYaml.put("cosim_config", config.cosimConfig);
    rootYaml.put("compiler", config.compiler);
    rootYaml.put("tests", testsYaml);

    Yaml yaml = new Yaml();
    try (var writer = new FileWriter(dest)) {
      yaml.dump(rootYaml, writer);
    }
  }

}
