package vadl.lcb.template.lld.ELF.Arch;

import java.io.IOException;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.superClass.AbstractEmitImmediateFilePass;

/**
 * This file is a helper file which includes all the lowered immediate files.
 */
public class EmitImmediateUtilsHeaderFilePass extends AbstractEmitImmediateFilePass {

  public EmitImmediateUtilsHeaderFilePass(LcbConfiguration lcbConfiguration) throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/lld/ELF/Arch/ImmediateUtils.h";
  }

  @Override
  protected String getOutputPath() {
    return "lld/ELF/Arch/ImmediateUtils.h";
  }
}
