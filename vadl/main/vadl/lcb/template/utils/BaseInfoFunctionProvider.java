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
        .filter(distinctByKey(x -> x.relocation().identifier))
        .filter(x -> x instanceof HasRelocationComputationAndUpdate)
        .map(x -> (HasRelocationComputationAndUpdate) x)
        //.sorted(Comparator.comparing(o -> o.elfRelocationName().value()))
        .map(relocation -> {
          var generator = new ValueRelocationFunctionCodeGenerator(relocation.valueRelocation(),
              new ValueRelocationFunctionCodeGenerator.Options(
                  false, true
              ));
          var function = new CppFunctionCode(generator.genFunctionDefinition());
          return new BaseInfoRecord(
              relocation.valueRelocation().identifier.lower(),
              function
          );
        })
        .toList();
  }

  private static <T> Predicate<T> distinctByKey(
      Function<? super T, ?> keyExtractor) {

    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }
}
