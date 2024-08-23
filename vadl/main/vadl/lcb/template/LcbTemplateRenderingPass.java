package vadl.lcb.template;

import java.io.IOException;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.LcbConfiguration;
import vadl.template.AbstractTemplateRenderingPass;

public abstract class LcbTemplateRenderingPass extends AbstractTemplateRenderingPass {
  public LcbTemplateRenderingPass(GeneralConfiguration configuration) throws IOException {
    super(configuration, "lcb");
  }

  public LcbConfiguration lcbConfiguration() {
    return (LcbConfiguration) configuration();
  }
}
