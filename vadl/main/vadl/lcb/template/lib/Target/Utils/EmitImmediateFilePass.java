package vadl.lcb.template.lib.Target.Utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForDecodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForEncodingsPass;
import vadl.gcb.passes.type_normalization.CppTypeNormalizationForPredicatesPass;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.codegen.DecodingCodeGenerator;
import vadl.lcb.codegen.EncoderDecoderCodeGenerator;
import vadl.lcb.codegen.EncodingCodeGenerator;
import vadl.lcb.codegen.PredicateCodeGenerator;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.pass.PassKey;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Definition;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Specification;

/**
 * This file contains all the immediates for TableGen.
 */
public class EmitImmediateFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitImmediateFilePass(LcbConfiguration lcbConfiguration, ProcessorName processorName)
      throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/Utils/ImmediateUtils.h";
  }

  @Override
  protected String getOutputPath() {
    return "lcb/llvm/lib/Target/" + processorName.value() + "/Utils/"
        + processorName.value() + "ImmediateUtils.h";
  }

  @Override
  protected Map<String, Object> createVariables(final Map<PassKey, Object> passResults,
                                                Specification specification) {
    var decodeVadlFunctions =
        (IdentityHashMap<Function, Function>) ensureNonNull(
            passResults.get(new PassKey(CppTypeNormalizationForDecodingsPass.class.toString())),
            "decodings must exist");
    var encodeVadlFunctions =
        (IdentityHashMap<Function, Function>) ensureNonNull(
            passResults.get(new PassKey(CppTypeNormalizationForEncodingsPass.class.toString())),
            "encodings must exist");
    var predicateVadlFunctions =
        (IdentityHashMap<Function, Function>) ensureNonNull(
            passResults.get(new PassKey(CppTypeNormalizationForPredicatesPass.class.toString())),
            "predicates must exist");

    var decodeFunctions = generateDecodeFunctions(specification, decodeVadlFunctions);
    var encodeFunctions = generateEncodeFunctions(specification, encodeVadlFunctions);
    var predicateFunctions = generatePredicateFunctions(specification, predicateVadlFunctions);

    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        "decodeFunctions", decodeFunctions,
        "decodeFunctionNames", generateDecodeFunctionNames(specification),
        "encodeFunctions", encodeFunctions,
        "predicateFunctions", predicateFunctions);
  }


  record DecodeFunctionEntry(String loweredName, String functionName) {

  }

  private List<DecodeFunctionEntry> generateDecodeFunctionNames(Specification specification) {
    return specification.isas().flatMap(isa -> isa.formats().stream())
        .flatMap(format -> Arrays.stream(format.fieldAccesses()))
        .map(Format.FieldAccess::accessFunction)
        .sorted(Comparator.comparing(Definition::name))
        .map(function -> {
          return new DecodeFunctionEntry(function.identifier.lower(),
              DecodingCodeGenerator.generateFunctionName(function.identifier.lower()));
        })
        .toList();
  }

  private List<String> generateDecodeFunctions(Specification specification,
                                               IdentityHashMap<Function, Function>
                                                   decodeVadlFunctions) {
    return specification.isas().flatMap(isa -> isa.formats().stream())
        .flatMap(format -> Arrays.stream(format.fieldAccesses()))
        .map(Format.FieldAccess::accessFunction)
        .sorted(Comparator.comparing(Definition::name))
        .map(function -> {
          var generator = new DecodingCodeGenerator();
          // We need to do a lookup because decodeVadlFunctions because it contains the cpp
          // upcasts.
          var upcasted = ensureNonNull(decodeVadlFunctions.get(function), "upcast must exist");
          return generator.generateFunction(upcasted);
        })
        .toList();
  }

  private List<String> generateEncodeFunctions(Specification specification,
                                               IdentityHashMap<Function, Function>
                                                   encodeVadlFunctions) {
    return specification.isas().flatMap(isa -> isa.formats().stream())
        .flatMap(format -> Arrays.stream(format.fieldAccesses()))
        .map(Format.FieldAccess::encoding)
        .sorted(Comparator.comparing(Definition::name))
        .map(function -> {
          var generator = new EncodingCodeGenerator();
          // We need to do a lookup because encodeVadlFunctions because it contains the cpp
          // upcasts.
          var upcasted = ensureNonNull(encodeVadlFunctions.get(function), "upcast must exist");
          return generator.generateFunction(upcasted);
        })
        .toList();
  }

  private List<String> generatePredicateFunctions(Specification specification,
                                                  IdentityHashMap<Function, Function>
                                                      predicateVadlFunctions) {
    return specification.isas().flatMap(isa -> isa.formats().stream())
        .flatMap(format -> Arrays.stream(format.fieldAccesses()))
        .map(Format.FieldAccess::predicate)
        .sorted(Comparator.comparing(Definition::name))
        .map(function -> {
          var generator = new PredicateCodeGenerator();
          // We need to do a lookup because encodeVadlFunctions because it contains the cpp
          // upcasts.
          var upcasted = ensureNonNull(predicateVadlFunctions.get(function), "upcast must exist");
          return generator.generateFunction(upcasted);
        })
        .toList();
  }
}
