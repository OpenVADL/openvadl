package vadl.iss.passes;

import java.io.IOException;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

public class IssConfigurationPass extends AbstractIssPass {

  public IssConfigurationPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Configuration Pass");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {
    var configuration = configuration();
    // TODO: Determine actual architecture name
    
    configuration.setArchitectureName("vadl");

    // we return the configuration but also manipulate the original one,
    // so the return is actually not necessary.
    return configuration;
  }
}
