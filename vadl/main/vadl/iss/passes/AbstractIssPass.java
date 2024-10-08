package vadl.iss.passes;

import vadl.configuration.IssConfiguration;
import vadl.pass.Pass;

/**
 * The pass all ISS (QEMU) passes extend from.
 * ISS template rendering passes do not extend from this class but from
 * {@link vadl.iss.template.IssTemplateRenderingPass}.
 */
public abstract class AbstractIssPass extends Pass {

  protected AbstractIssPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public IssConfiguration configuration() {
    return (IssConfiguration) super.configuration();
  }
}
