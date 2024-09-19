package vadl.iss.passes;

import vadl.configuration.IssConfiguration;
import vadl.pass.Pass;

public abstract class AbstractIssPass extends Pass {

  protected AbstractIssPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public IssConfiguration configuration() {
    return (IssConfiguration) super.configuration();
  }
}
