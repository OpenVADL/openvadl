package vadl.lcb.template.utils;

import java.util.Map;
import java.util.stream.Collectors;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForPredicatesPass;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Format;

public class ImmediatePredicateFunctionProvider {
  public static Map<Format.Field, CppFunction> generatePredicateFunctions(
      PassResults passResults) {
    return ((CppTypeNormalizationPass.NormalisedTypeResult)
        passResults.lastResultOf(CppTypeNormalizationForPredicatesPass.class))
        .fields()
        .stream()
        .map(x -> new Pair<>(x.getKey(), new CppFunction(x.getValue(), "predicate")))
        .collect(Collectors.toMap(Pair::left, Pair::right));
  }
}
