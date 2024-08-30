package vadl.lcb.template.lib.Target.AsmParser;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.assemblyConstantIntern.AssemblyConstant;
import vadl.lcb.codegen.assembly.ParserGenerator;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file includes the definitions for the asm parser.
 */
public class EmitAsmRecursiveDescentParserCppFilePass extends LcbTemplateRenderingPass {

  public EmitAsmRecursiveDescentParserCppFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/AsmParser/AsmRecursiveDescentParser.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/lib/Target/" + lcbConfiguration().processorName().value()
        + "/AsmParser/AsmRecursiveDescentParser.cpp";
  }

  record ParsingResultLex(AssemblyConstant.TOKEN_KIND kind, String functionName) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var parsingResultsConstants =
        specification.isas().flatMap(isa -> isa.instructions().stream())
            .flatMap(instruction -> instruction.assembly().function().behavior().getNodes(
                AssemblyConstant.class))
            .sorted(Comparator.comparing(AssemblyConstant::kind)) // Sort by something
            .map(assemblyConstant -> new ParsingResultLex(
                assemblyConstant.kind(),
                ParserGenerator.generateConstantName(assemblyConstant)))
            .toList();
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        "lexParsingResults", parsingResultsConstants);
  }
}
