package vadl.gcb.passes.pseudo;

import java.util.stream.Stream;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;

/**
 * Expand "real" pseudo instructions which are defined in the specification.
 */
public class PseudoExpansionFunctionGeneratorPass
    extends AbstractPseudoExpansionFunctionGeneratorPass {
  public PseudoExpansionFunctionGeneratorPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("PseudoExpansionFunctionGeneratorPass");
  }

  @Override
  protected Stream<PseudoInstruction> getApplicable(PassResults passResults, Specification viam) {
    return viam.isa()
        .map(isa -> isa.ownPseudoInstructions().stream()).orElseGet(Stream::empty);
  }
}
