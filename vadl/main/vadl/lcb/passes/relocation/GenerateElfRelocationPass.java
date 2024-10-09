package vadl.lcb.passes.relocation;

import java.io.IOException;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.gcb.passes.relocation.GenerateLogicalRelocationPass;
import vadl.gcb.passes.relocation.model.ElfRelocation;
import vadl.gcb.passes.relocation.model.LogicalRelocation;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * Generate {@link ElfRelocation} based on {@link LogicalRelocation}.
 */
public class GenerateElfRelocationPass extends Pass {
  public GenerateElfRelocationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("generateElfRelocationPass");
  }

  @Nullable
  @Override
  public List<ElfRelocation> execute(PassResults passResults, Specification viam)
      throws IOException {
    var logicalRelocations =
        (GenerateLogicalRelocationPass.Output) passResults.lastResultOf(
            GenerateLogicalRelocationPass.class);

    return logicalRelocations.all().stream()
        .map(ElfRelocation::new)
        .toList();
  }
}
