package vadl.viam.passes.sideeffect_condition;

import java.io.IOException;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.GraphUtils;
import vadl.utils.ViamUtils;
import vadl.viam.Instruction;
import vadl.viam.Specification;

public class SideEffectConditionResolvingPass extends Pass {

  public SideEffectConditionResolvingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("Side-Effect Condition Resolving Pass");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {
    var instructions = ViamUtils
        .findDefinitionByFilter(viam, d -> d instanceof Instruction);
    for (var instruction : instructions) {
      SideEffectConditionResolver.run(((Instruction) instruction).behavior());
    }

    return null;
  }
}
