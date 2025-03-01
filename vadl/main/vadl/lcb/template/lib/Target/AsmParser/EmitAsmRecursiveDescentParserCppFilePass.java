package vadl.lcb.template.lib.Target.AsmParser;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.codegen.assembly.AssemblyParserCodeGenerator;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;
import vadl.viam.annotations.AsmParserCaseSensitive;

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

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var grammarRules = grammarRules(specification);
    var compareFunction = stringCompareFunction(specification);
    return Map.of(CommonVarNames.NAMESPACE,
        lcbConfiguration().processorName().value().toLowerCase(),
        "asmDescriptionExists", specification.assemblyDescription().isPresent(),
        "grammarRules", grammarRules,
        "compareFunction", compareFunction
    );
  }

  private String stringCompareFunction(Specification specification) {
    var isCaseSensitive = specification.assemblyDescription()
        .map(asmDesc -> asmDesc.annotation(AsmParserCaseSensitive.class))
        .map(AsmParserCaseSensitive::isCaseSensitive).orElse(false);

    return isCaseSensitive ? "equals" : "equals_insensitive";
  }

  private String grammarRules(Specification specification) {
    var parserCaseSensitive = specification.assemblyDescription()
        .map(asmDesc -> asmDesc.annotation(AsmParserCaseSensitive.class))
        .map(AsmParserCaseSensitive::isCaseSensitive).orElse(false);

    var rules = specification.assemblyDescription()
        .map(asmDesc -> asmDesc.rules().stream())
        .orElse(Stream.empty());

    var codeGenerator = new AssemblyParserCodeGenerator(
        lcbConfiguration().processorName().value().toLowerCase(), parserCaseSensitive, rules
    );

    return codeGenerator.generateRules();
  }
}
