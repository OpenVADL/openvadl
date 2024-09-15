package vadl.lcb.template;

import java.io.IOException;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.template.AbstractTemplateRenderingPass;
import vadl.viam.RegisterFile;

/**
 * Abstracts the subdir under the output.
 */
public abstract class LcbTemplateRenderingPass extends AbstractTemplateRenderingPass {
  public LcbTemplateRenderingPass(GeneralConfiguration configuration) throws IOException {
    super(configuration, "lcb");
  }

  public LcbConfiguration lcbConfiguration() {
    return (LcbConfiguration) configuration();
  }

  protected String renderRegister(RegisterFile registerFile, int addr) {
    return registerFile.identifier.simpleName() + addr;
  }
}
