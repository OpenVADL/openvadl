package vadl.iss.template;

import vadl.configuration.IssConfiguration;

/**
 * An ISS template rendering pass that takes the pass to an template and renders it
 * with the default variables set by the {@link IssTemplateRenderingPass}.
 * This reduces the number of required rendering passes and makes the pass order more
 * readable, especially when using the {@link #issDefault(String, IssConfiguration)}
 * constructor.
 *
 * @see vadl.pass.PassOrder#iss
 */
public class IssDefaultRenderingPass extends IssTemplateRenderingPass {

  private final String issTemplatePath;

  public IssDefaultRenderingPass(String issTemplatePath, IssConfiguration configuration) {
    super(configuration);
    this.issTemplatePath = issTemplatePath;
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
