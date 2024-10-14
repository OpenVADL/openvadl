package vadl.test.iss;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public class IssTestUtils {

  protected record TestSpec(
      String id,
      Map<String, String> regTests,
      String asmCore
  ) {
  }

  protected record TestResult(
      String id,
      TestResult.Status status,
      List<TestResult.Stage> completedStages,
      List<TestResult.RegTestResult> regTests,
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
      RUN
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
   *
   * @param specs The collection of test specifications.
   * @param dest  The destination file to write the YAML configuration.
   * @throws IOException if an I/O error occurs.
   */
  protected static void writeTestSuiteConfigYaml(Collection<TestSpec> specs,
                                                 File dest)
      throws IOException {
    var specsYaml = specs.stream().map(spec -> {
      var specYaml = new LinkedHashMap<String, Object>();
      specYaml.put("id", spec.id);
      specYaml.put("reg_tests", spec.regTests);
      specYaml.put("asm_core", spec.asmCore);
      return specYaml;
    }).toList();

    var yamlSuite = new LinkedHashMap<>();
    yamlSuite.put("tests", specsYaml);
    Yaml yaml = new Yaml();
    try (var writer = new FileWriter(dest)) {
      yaml.dump(yamlSuite, writer);
    }
  }

  /**
   * Converts a YAML file to a TestResult object.
   *
   * @param yamlFile The YAML file to convert.
   * @return The converted TestResult object.
   * @throws IOException if an I/O error occurs.
   */
  protected static List<TestResult> yamlToTestResults(File yamlFile)
      throws IOException {
    try (var reader = new FileInputStream(yamlFile)) {
      Yaml yaml = new Yaml();
      // load all results
      List<Object> results = yaml.load(reader);

      return results.stream()
          // map yaml to test result
          .map(r -> {
            Map<String, Object> data = (Map<String, Object>) r;
            Map<String, Object> result = (Map<String, Object>) data.get("result");

            String id = data.get("id").toString();
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
                          val.get("expected"), val.get("actual"));
                    }).toList();

            return new TestResult(id, status, completedStages, regTests, errors,
                duration);
          })
          .toList();
    }
  }

}
