package vadl.iss.passes;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * Sets configurations in the {@link IssConfiguration} if the information must be determined
 * from the VIAM.
 */
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

    var isaName = viam.mip().get().targetName().toLowerCase();
    configuration.setTargetName(isaName);

    viam.isa().ifPresent(isa -> {
      var targetSize = requireNonNull(isa.pc()).registerResource().resultType().bitWidth();
      configuration.setTargetSize(Tcg_32_64.fromWidth(targetSize));
    });

    // we return the configuration but also manipulate the original one,
    // so the return is actually not necessary.
    return configuration;
  }
}
