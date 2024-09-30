package vadl.lcb.template.lld.ELF.Arch;

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
 * This files defines the relocations for the linker.
 */
public class EmitLldArchFilePass extends LcbTemplateRenderingPass {

  public EmitLldArchFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/lld/ELF/Arch/Target.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "lld/ELF/Arch/" + lcbConfiguration().processorName().value() + ".cpp";
  }

  record ElfInfo(boolean isBigEndian, int maxInstructionWordSize) {

  }

  private ElfInfo createElfInfo() {
    return new ElfInfo(false, 32);
  }

  @Override
  protected Map<String, Object> createVariables(final PassResults passResults,
                                                Specification specification) {
    var relocations =
        (List<ElfRelocation>) passResults.lastResultOf(GenerateElfRelocationPass.class);
    var elfInfo = createElfInfo();
    return Map.of(CommonVarNames.NAMESPACE, specification.simpleName(),
        CommonVarNames.MAX_INSTRUCTION_WORDSIZE, elfInfo.maxInstructionWordSize(),
        CommonVarNames.IS_BIG_ENDIAN, elfInfo.isBigEndian(),
        CommonVarNames.RELOCATIONS, relocations);
  }
}
