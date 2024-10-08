package vadl.lcb.include.llvm.BinaryFormat.ELFRelocs;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import vadl.configuration.LcbConfiguration;
import vadl.gcb.passes.relocation.model.ElfRelocation;
import vadl.lcb.passes.relocation.GenerateElfRelocationPass;
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

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var relocations =
        (List<ElfRelocation>) passResults.lastResultOf(GenerateElfRelocationPass.class);

    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        CommonVarNames.RELOCATIONS, relocations);
  }
}
