package vadl.viam.passes;

import java.io.IOException;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

public class InstructionResourceAccessAnalysisPass extends Pass {


  protected InstructionResourceAccessAnalysisPass(
      GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Instruction Resource Access Analysis Pass");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {


    return null;
  }
}

