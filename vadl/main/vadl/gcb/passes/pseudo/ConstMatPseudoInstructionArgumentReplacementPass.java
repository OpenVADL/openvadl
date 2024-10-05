package vadl.gcb.passes.pseudo;

import java.util.List;
import java.util.stream.Stream;
import vadl.configuration.GeneralConfiguration;
import vadl.lcb.passes.llvmLowering.domain.ConstantMatPseudoInstruction;
import vadl.lcb.passes.llvmLowering.immediates.GenerateConstantMaterialisationPass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;

/**
 * Applies the arguments of an {@link Instruction} of a {@link PseudoInstruction} but only
 * for constant materialisation pseudo instructions.
 */
public class ConstMatPseudoInstructionArgumentReplacementPass
    extends AbstractPseudoInstructionArgumentReplacementPass {
  public ConstMatPseudoInstructionArgumentReplacementPass(
      GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected Stream<PseudoInstruction> getApplicable(PassResults passResults, Specification viam) {
    var constMats = (List<ConstantMatPseudoInstruction>) passResults.lastResultOf(
        GenerateConstantMaterialisationPass.class);
    return constMats
        .stream()
        .map(x -> x);
  }

  @Override
  public PassName getName() {
    return new PassName("ConstMatPseudoInstructionArgumentReplacementPass");
  }
}
