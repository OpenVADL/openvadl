package vadl.iss.template;

import vadl.configuration.IssConfiguration;
import vadl.pass.PassName;

public class IssDefaultRenderingPass extends IssTemplateRenderingPass {

  private final String issTemplatePath;

  public IssDefaultRenderingPass(String issTemplatePath, IssConfiguration configuration) {
    super(configuration);
    this.issTemplatePath = issTemplatePath;
  }

  @Override
  public PassName getName() {
    return PassName.of("Rendering ISS " + issTemplatePath);
  }

  @Override
  protected String issTemplatePath() {
    return issTemplatePath;
  }

  public static IssDefaultRenderingPass issDefault(String issTemplatePath,
                                                   IssConfiguration config) {
    return new IssDefaultRenderingPass(issTemplatePath, config);
  }
}
