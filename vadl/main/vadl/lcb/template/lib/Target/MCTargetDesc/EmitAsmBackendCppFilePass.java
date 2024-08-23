package vadl.lcb.template.lib.Target.MCTargetDesc;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * This file contains the implementation for assembly fixups.
 */
public class EmitAsmBackendCppFilePass extends LcbTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitAsmBackendCppFilePass(LcbConfiguration lcbConfiguration, ProcessorName processorName)
      throws IOException {
    super(lcbConfiguration);
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/MCTargetDesc/AsmBackend.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/lib/Target/" + processorName.value() + "/MCTargetDesc/" + processorName.value()
        + "AsmBackend.cpp";
  }

  enum RelocationKind {
    ABSOLUTE,
    PC_RELATIVE,
  }

  record Relocation(String mcFixupKindIdentifier,
                    String functionIdentifier,
                    RelocationKind kind) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        CommonVarNames.RELOCATIONS, List.of(
            new Relocation("fixupKindIdentifierValue",
                "functionIdentifierValue",
                RelocationKind.PC_RELATIVE)));
  }
}
