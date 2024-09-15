package vadl.lcb.template.lib.Target.Utils;

import java.io.IOException;
import vadl.configuration.LcbConfiguration;
import vadl.lcb.template.superClass.AbstractEmitImmediateFilePass;

/**
 * This file contains all the immediates for TableGen.
 */
public class EmitImmediateFilePass extends AbstractEmitImmediateFilePass {

  public EmitImmediateFilePass(LcbConfiguration lcbConfiguration)
      throws IOException {
    super(lcbConfiguration);
  }

  @Override
  protected String getTemplatePath() {
    return "lcb/llvm/lib/Target/Utils/ImmediateUtils.h";
  }

  @Override
  protected String getOutputPath() {
    var processorName = lcbConfiguration().processorName().value();
    return "llvm/lib/Target/" + processorName + "/Utils/ImmediateUtils.h";
  }
}
