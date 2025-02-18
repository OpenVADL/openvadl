package vadl.lcb.passes.pseudo;

import java.io.IOException;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.gcb.passes.pseudo.AbstractPseudoInstructionArgumentReplacementPass;
import vadl.gcb.passes.pseudo.PseudoInstructionArgumentReplacementPass;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.types.SIntType;
import vadl.viam.Constant;
import vadl.viam.Specification;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.dependency.ConstantNode;

/**
 * LLVM only implements fixed width immediates. Therefore, we need to
 * uplift any constant with an arbitrary bit width.
 * Note it does not mutate the graph on the viam, but only the result of
 * {@link PseudoInstructionArgumentReplacementPass}.
 */
public class PseudoConstantUpliftingPass extends Pass {
  public PseudoConstantUpliftingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("PseudoConstantUpliftingPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var pseudoInstructions = viam.isa()
        .map(isa -> isa.ownPseudoInstructions().stream()).orElseGet(Stream::empty)
        .toList();
    var appliedArguments =
        (AbstractPseudoInstructionArgumentReplacementPass.Output) passResults.lastResultOf(
            PseudoInstructionArgumentReplacementPass.class);

    for (var pseudoInstruction : pseudoInstructions) {
      var behaviorWithAppliedArguments =
          appliedArguments.appliedGraph().get(pseudoInstruction);
      if (behaviorWithAppliedArguments != null) {
        behaviorWithAppliedArguments
            .getNodes(InstrCallNode.class)
            .map(instrCallNode -> instrCallNode.target().behavior())
            .flatMap(graph -> graph.getNodes(ConstantNode.class))
            .forEach(constantNode -> {
              if (constantNode.type() instanceof BitsType bitsType) {
                if (bitsType.bitWidth() <= 32) {
                  var ty = bitsType.withBitWidth(32);
                  constantNode.setType(ty);
                  constantNode.constant().setType(ty);
                } else if (bitsType.bitWidth() <= 64) {
                  var ty = bitsType.withBitWidth(64);
                  constantNode.setType(ty);
                  constantNode.constant().setType(ty);
                } else {
                  throw Diagnostic.error("Not supporting constants larger than 64",
                      constantNode.sourceLocation()).build();
                }
              }
            });
      }
    }

    return null;
  }
}
