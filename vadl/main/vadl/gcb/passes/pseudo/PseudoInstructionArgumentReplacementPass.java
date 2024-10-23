package vadl.gcb.passes.pseudo;

import java.util.Collections;
import java.util.stream.Stream;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;
import vadl.viam.passes.dummyAbi.DummyAbi;

/**
 * Applies the arguments of an {@link Instruction} of a {@link PseudoInstruction}.
 */
public class PseudoInstructionArgumentReplacementPass
    extends AbstractPseudoInstructionArgumentReplacementPass {
  public PseudoInstructionArgumentReplacementPass(
      GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected Stream<PseudoInstruction> getApplicable(PassResults passResults, Specification viam) {
    var abi = (DummyAbi) viam.definitions().filter(x -> x instanceof DummyAbi).findFirst().get();
    // Pseudo Instructions + ABI sequences
    return
        Stream.concat(
            viam.isa()
                .map(InstructionSetArchitecture::ownPseudoInstructions)
                .orElse(Collections.emptyList())
                .stream(),
            Stream.of(abi.returnSequence(), abi.callSequence())
        );
  }

  @Override
  public PassName getName() {
    return new PassName("PseudoInstructionArgumentReplacementPass");
  }
}
