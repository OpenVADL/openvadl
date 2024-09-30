package vadl.lcb.template.superClass;

import static vadl.lcb.template.utils.ImmediateDecodingFunctionProvider.generateDecodeFunctions;
import static vadl.lcb.template.utils.ImmediateEncodingFunctionProvider.generateEncodeFunctions;
import static vadl.lcb.template.utils.ImmediatePredicateFunctionProvider.generatePredicateFunctions;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.model.CppFunction;
import vadl.cppCodeGen.model.CppFunctionCode;
import vadl.cppCodeGen.model.CppFunctionName;
import vadl.lcb.codegen.LcbCodeGenerator;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file contains all the immediates for TableGen and the Linker.
 */
public abstract class AbstractEmitImmediateFilePass extends LcbTemplateRenderingPass {

  public AbstractEmitImmediateFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var decodeFunctions = generateDecodeFunctions(passResults);
    var encodeFunctions = generateEncodeFunctions(passResults);
    var predicateFunctions = generatePredicateFunctions(passResults);

    var decodeFunctionNames = decodeFunctions
        .values()
        .stream()
        .map(CppFunction::functionName)
        .map(CppFunctionName::lower)
        .sorted()
        .toList();

    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "decodeFunctions",
        decodeFunctions.values().stream().map(x -> new LcbCodeGenerator().generateFunction(x))
            .sorted(Comparator.comparing(CppFunctionCode::value))
            .toList(),
        "decodeFunctionNames", decodeFunctionNames,
        "encodeFunctions",
        encodeFunctions.values().stream().map(x -> new LcbCodeGenerator().generateFunction(x))
            .sorted(Comparator.comparing(CppFunctionCode::value))
            .toList(),
        "predicateFunctions", predicateFunctions.values().stream()
            .map(x -> new LcbCodeGenerator().generateFunction(x))
            .sorted(Comparator.comparing(CppFunctionCode::value))
            .toList());
  }
}
