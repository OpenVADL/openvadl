package vadl.lcb.lib.Target.AsmParser;

import java.io.IOException;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * This file includes the definitions for the asm parser.
 */
public class EmitAsmRecursiveDescentParserCppFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitAsmRecursiveDescentParserCppFilePass(LcbConfiguration lcbConfiguration,
                                                  ProcessorName processorName) throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/AsmParser/AsmRecursiveDescentParser.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/lib/Target/" + processorName.value() + "/AsmParser/AsmRecursiveDescentParser.cpp";
  }

  @Override
  protected Map<String, Object> createVariables(Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
