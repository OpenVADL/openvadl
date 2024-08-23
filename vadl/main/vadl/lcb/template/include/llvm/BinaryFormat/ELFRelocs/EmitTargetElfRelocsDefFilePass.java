package vadl.lcb.include.llvm.BinaryFormat.ELFRelocs;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.CommonVarNames;
import vadl.lcb.template.LcbTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * This file creates the ELF relocation definitions.
 */
public class EmitTargetElfRelocsDefFilePass extends LcbTemplateRenderingPass {

  public EmitTargetElfRelocsDefFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/include/llvm/BinaryFormat/ELFRelocs/Target.def";
  }

  @Override
  protected String getOutputPath() {
    return "llvm/include/llvm/BinaryFormat/ELFRelocs/"
        + lcbConfiguration().processorName().value() + ".def";
  }

  record Relocation(String identifier) {

  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        CommonVarNames.RELOCATIONS, List.of(new Relocation("relocationIdentifierValue")));
  }
}
