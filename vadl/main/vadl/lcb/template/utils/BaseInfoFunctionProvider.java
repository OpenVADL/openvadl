package vadl.lcb.template.utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import vadl.cppCodeGen.common.ValueRelocationFunctionCodeGenerator;
import vadl.cppCodeGen.model.CppFunctionCode;
import vadl.gcb.passes.relocation.model.HasRelocationComputationAndUpdate;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.pass.PassResults;
import vadl.template.Renderable;

/**
 * Helper class for baseInfo.
 */
public class BaseInfoFunctionProvider {
  /**
   * A Base Info entry.
   */
  public record BaseInfoRecord(
      String functionName,
      CppFunctionCode relocation) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "functionName", functionName,
          "relocation", relocation
      );
    }
  }

  /**
   * Get the records.
   */
  public static List<BaseInfoRecord> getBaseInfoRecords(PassResults passResults) {
    var output =
        (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
            GenerateLinkerComponentsPass.class);
    var elfRelocations = output.elfRelocations();
    return elfRelocations.stream()
        .map(relocation -> {
          var generator =
              new ValueRelocationFunctionCodeGenerator(relocation, relocation.valueRelocation(),
                  new ValueRelocationFunctionCodeGenerator.Options(
                      false, true
                  ));
          var function = new CppFunctionCode(generator.genFunctionDefinition());
          return new BaseInfoRecord(
              generator.genFunctionName(),
              function
          );
        })
        .toList();
  }
}
