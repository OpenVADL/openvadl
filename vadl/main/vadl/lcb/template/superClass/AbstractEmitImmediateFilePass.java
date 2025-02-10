package vadl.lcb.template.superClass;

import static vadl.lcb.template.utils.ImmediateDecodingFunctionProvider.generateDecodeFunctions;
import static vadl.lcb.template.utils.ImmediateEncodingFunctionProvider.generateEncodeFunctions;
import static vadl.lcb.template.utils.ImmediatePredicateFunctionProvider.generatePredicateFunctions;

import java.io.IOException;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.common.GcbAccessOrExtractionFunctionCodeGenerator;
import vadl.cppCodeGen.model.CppFunctionName;
import vadl.cppCodeGen.model.GcbFieldAccessCppFunction;
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
        .map(GcbFieldAccessCppFunction::functionName)
        .map(CppFunctionName::lower)
        .sorted()
        .toList();

    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        "decodeFunctions",
        decodeFunctions.values().stream().map(x ->
                new GcbAccessOrExtractionFunctionCodeGenerator(x,
                    x.fieldAccess(),
                    x.identifier.lower(),
                    x.fieldAccess().fieldRef().simpleName()).genFunctionDefinition())
            .sorted()
            .toList(),
        "decodeFunctionNames", decodeFunctionNames,
        "encodeFunctions",
        encodeFunctions.values().stream()
            .map(x -> new GcbAccessOrExtractionFunctionCodeGenerator(x, x.fieldAccess(),
                x.identifier.lower(),
                x.fieldAccess().fieldRef().simpleName()).genFunctionDefinition())
            .sorted()
            .toList(),
        "predicateFunctions", predicateFunctions.values().stream()
            .map(x -> new GcbAccessOrExtractionFunctionCodeGenerator(x, x.fieldAccess(),
                x.identifier.lower(),
                x.fieldAccess().fieldRef().simpleName()).genFunctionDefinition())
            .sorted()
            .toList());
  }
}
