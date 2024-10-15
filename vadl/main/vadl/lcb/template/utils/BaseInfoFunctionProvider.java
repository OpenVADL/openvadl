package vadl.lcb.template.utils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import vadl.cppCodeGen.model.CppFunctionCode;
import vadl.cppCodeGen.model.VariantKind;
import vadl.gcb.passes.relocation.model.ElfRelocationName;
import vadl.gcb.passes.relocation.model.RelocationLowerable;
import vadl.lcb.codegen.LcbGenericCodeGenerator;
import vadl.lcb.passes.relocation.GenerateLinkerComponentsPass;
import vadl.pass.PassResults;

public class BaseInfoFunctionProvider {
  public record BaseInfoRecord(
      String functionName,
      CppFunctionCode relocation,
      VariantKind variantKind) {

  }

  public static List<BaseInfoRecord> getBaseInfoRecords(PassResults passResults) {
    var output =
        (GenerateLinkerComponentsPass.Output) passResults.lastResultOf(
            GenerateLinkerComponentsPass.class);
    var elfRelocations = output.elfRelocations();
    return elfRelocations.stream()
        .filter(distinctByKey(x -> x.relocation().identifier))
        .filter(x -> x instanceof RelocationLowerable)
        .map(x -> (RelocationLowerable) x)
        .sorted(Comparator.comparing(o -> o.elfRelocationName().value()))
        .map(relocation -> {
          var generator = new LcbGenericCodeGenerator();
          var function = generator.generateFunction(
              relocation.valueRelocation(),
              new LcbGenericCodeGenerator.Options(false, true));
          return new BaseInfoRecord(
              relocation.valueRelocation().identifier.lower(),
              function,
              relocation.variantKind()
          );
        })
        .toList();
  }

  static <T> Predicate<T> distinctByKey(
      Function<? super T, ?> keyExtractor) {

    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }
}
