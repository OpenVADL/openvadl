package vadl.lcb.include.llvm.BinaryFormat.ELFRelocs;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.lcb.config.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.pass.PassKey;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.Specification;

/**
 * This file creates the ELF relocation definitions.
 */
public class EmitTargetElfRelocsDefFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitTargetElfRelocsDefFilePass(LcbConfiguration lcbConfiguration,
                                        ProcessorName processorName) throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/include/llvm/BinaryFormat/ELFRelocs/Target.def";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/include/llvm/BinaryFormat/ELFRelocs/" + processorName.value() + ".def";
  }

  record Relocation(String identifier) {

  }

  @Override
  protected Map<String, Object> createVariables(final Map<PassKey, Object> passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        CommonVarNames.RELOCATIONS, List.of(new Relocation("relocationIdentifierValue")));
  }
}
