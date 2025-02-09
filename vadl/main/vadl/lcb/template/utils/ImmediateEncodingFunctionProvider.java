package vadl.lcb.template.utils;

import java.util.Map;
import java.util.stream.Collectors;
import vadl.cppCodeGen.model.GcbFieldAccessCppFunction;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.gcb.passes.typeNormalization.CppTypeNormalizationForEncodingsPass;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Format;

/**
 * Utility class for encodings.
 */
public class ImmediateEncodingFunctionProvider {
  /**
   * Get the encoding functions.
   */
  public static Map<Format.Field, GcbFieldAccessCppFunction> generateEncodeFunctions(
      PassResults passResults) {
    return ((CppTypeNormalizationPass.NormalisedTypeResult)
        passResults.lastResultOf(CppTypeNormalizationForEncodingsPass.class))
        .fields()
        .stream()
        .map(x -> new Pair<>(x.getKey(), x.getValue()))
        .collect(Collectors.toMap(Pair::left, Pair::right));
  }
}
