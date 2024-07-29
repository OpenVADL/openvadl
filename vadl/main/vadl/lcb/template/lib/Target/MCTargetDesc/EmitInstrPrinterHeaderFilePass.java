package vadl.lcb.lib.Target.MCTargetDesc;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * This file contains the definitions for emitting asm instructions.
 */
public class EmitInstrPrinterHeaderFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitInstrPrinterHeaderFilePass(LcbConfiguration lcbConfiguration,
                                        ProcessorName processorName) throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/TargetInstrPrinter.h";
  }

  @Override
  protected String getOutputPath() {
    return "lcb/llvm/lib/Target/" + processorName.value() + "/MCTargetDesc/"
        + processorName.value() + "InstrPrinter.h";
  }

  record Register(String name) {

  }

  record RegisterClass(String simpleName) {

  }

  @Override
  protected Map<String, Object> createVariables(Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        CommonVarNames.SYSTEM_REGISTERS, List.of(new Register("CSR")),
        CommonVarNames.REGISTERS_CLASSES, List.of(new RegisterClass("X")));
  }
}
