package vadl.lcb.lld.ELF.Arch;

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
 * This files defines the relocations for the linker.
 */
public class EmitLldArchFilePass extends AbstractTemplateRenderingPass {

  private final ProcessorName processorName;

  public EmitLldArchFilePass(LcbConfiguration lcbConfiguration,
                             ProcessorName processorName)
      throws IOException {
    super(lcbConfiguration.outputPath());
    this.processorName = processorName;
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/lld/ELF/Arch/Target.cpp";
  }

  @Override
  protected String getOutputPath() {
    return "lld/ELF/Arch/" + processorName.value() + ".cpp";
  }

  record ElfInfo(boolean isBigEndian, int maxInstructionWordSize) {

  }

  record Relocation(String identifier, String relExpr, RelocationKind kind) {

  }

  enum RelocationKind {
    ABSOLUTE,
    PC_RELATIVE,
  }

  private ElfInfo createElfInfo() {
    return new ElfInfo(false, 32);
  }

  private List<Relocation> createRelocation() {
    return List.of(new Relocation("identifierValue", "relExprValue", RelocationKind.ABSOLUTE));
  }

  @Override
  protected Map<String, Object> createVariables(final Map<PassKey, Object> passResults,
                                                Specification specification) {
    var elfInfo = createElfInfo();
    return Map.of(CommonVarNames.NAMESPACE, specification.name(),
        CommonVarNames.MAX_INSTRUCTION_WORDSIZE, elfInfo.maxInstructionWordSize(),
        CommonVarNames.IS_BIG_ENDIAN, elfInfo.isBigEndian(),
        CommonVarNames.RELOCATIONS, createRelocation());
  }
}
