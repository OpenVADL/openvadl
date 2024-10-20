package vadl.gcb.passes.pseudo;

import static vadl.viam.ViamError.ensureNonNull;

import java.util.Objects;
import java.util.stream.Stream;
import vadl.configuration.GeneralConfiguration;
import vadl.error.Diagnostic;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.Pair;
import vadl.viam.PseudoInstruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.passes.dummyAbi.DummyAbi;

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
  protected Stream<Pair<PseudoInstruction, Graph>> getApplicable(
      PassResults passResults,
      Specification viam) {
    var abi = (DummyAbi) viam.definitions().filter(x -> x instanceof DummyAbi).findFirst().get();
    var appliedArguments =
        (AbstractPseudoInstructionArgumentReplacementPass.Output) passResults.lastResultOf(
            PseudoInstructionArgumentReplacementPass.class);

    var specifiedSequences = Stream.of(abi.returnSequence(), abi.callSequence());

    // We do not use the behavior of the pseudo instruction because each InstrCallNode
    // has the instruction behavior to the original instruction.
    // However, we did apply the arguments, and now we want to expand the pseudo instruction
    // with those arguments. That's why we have to use the result of a previous pass.
    var pseudoInstructions = Stream.concat(viam.isa()
            .map(isa -> isa.ownPseudoInstructions().stream()).orElseGet(Stream::empty),
        specifiedSequences);
    return pseudoInstructions
        .map(pseudoInstruction -> {
          var appliedInstructions = appliedArguments.appliedGraph().get(pseudoInstruction);
          ensureNonNull(appliedInstructions,
              () -> Diagnostic.error("There is no graph with the applied arguments.",
                  pseudoInstruction.sourceLocation()));
          return Pair.of(pseudoInstruction, Objects.requireNonNull(appliedInstructions));
        });
  }
}
