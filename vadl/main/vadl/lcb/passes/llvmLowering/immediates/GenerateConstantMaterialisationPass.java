package vadl.lcb.passes.llvmLowering.immediates;


import java.io.IOException;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.lcb.passes.isaMatching.IsaMatchingPass;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;

/**
 * This pass generates {@link PseudoInstruction} which consume immediates and emit a machine
 * instruction which loads the value into the register. Note that we will not be looking for
 * instruction like {@code LI} because it is hard to capture the semantics. Instead, we use
 * {@link IsaMatchingPass} and use the {@code ADDI} as move-instruction.
 */
public class GenerateConstantMaterialisationPass extends Pass {

  public GenerateConstantMaterialisationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateConstantMaterialisationPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    return null;
  }
}
