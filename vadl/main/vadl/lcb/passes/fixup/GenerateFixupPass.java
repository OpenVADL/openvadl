package vadl.lcb.passes.fixup;

import java.io.IOException;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.gcb.passes.relocation.GenerateLogicalRelocationPass;
import vadl.gcb.passes.relocation.model.LogicalRelocation;
import vadl.lcb.passes.fixup.domain.Fixup;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * Generate {@link Fixup} based on {@link LogicalRelocation}.
 */
public class GenerateFixupPass extends Pass {
  public GenerateFixupPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateFixupPass");
  }

  @Nullable
  @Override
  public List<Fixup> execute(PassResults passResults, Specification viam)
      throws IOException {
    var logicalRelocations =
        (List<LogicalRelocation>) passResults.lastResultOf(GenerateLogicalRelocationPass.class);

    return logicalRelocations.stream()
        .map(Fixup::new)
        .toList();
  }
}
