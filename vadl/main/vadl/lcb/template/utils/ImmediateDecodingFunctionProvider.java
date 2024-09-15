package vadl.lcb.template.utils;

import java.util.Map;
import java.util.stream.Collectors;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.passes.typeNormalization.CppTypeNormalizationPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForDecodingsPass;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.Format;

/**
 * Utility class for decodings.
 */
public class ImmediateDecodingFunctionProvider {
  /**
   * Get the decoding functions.
   */
  public static Map<Format.Field, CppFunction> generateDecodeFunctions(PassResults passResults) {
    return ((CppTypeNormalizationPass.NormalisedTypeResult)
        passResults.lastResultOf(CppTypeNormalizationForDecodingsPass.class))
        .fields()
        .stream()
        .map(x -> new Pair<>(x.getKey(), new CppFunction(x.getValue())))
        .collect(Collectors.toMap(Pair::left, Pair::right));
  }
}
