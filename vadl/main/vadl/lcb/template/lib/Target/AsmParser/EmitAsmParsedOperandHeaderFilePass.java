package vadl.lcb.template.lib.Target.AsmParser;

import java.io.IOException;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.pass.PassKey;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * This file contains the definitions for parsing assembly files.
 */
public class EmitAsmParsedOperandHeaderFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitAsmParsedOperandHeaderFilePass(LcbConfiguration lcbConfiguration,
                                            ProcessorName processorName) throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/Target/AsmParser/AsmParsedOperand.h";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/Target/" + processorName.value() + "/AsmParser/AsmParsedOperand.h";
  }

  @Override
  protected Map<String, Object> createVariables(final Map<PassKey, Object> passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name());
  }
}
