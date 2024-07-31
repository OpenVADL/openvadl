package vadl.lcb.passes;

import java.io.IOException;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.TypeCastNode;

/**
 * This pass removes {@link Node} of {@link Instruction#behavior()} which are not useful for the
 * lowering.
 */
public class InstructionBehaviorSimplificationPass extends Pass {
  @Override
  public PassName getName() {
    return new PassName("instructionBehaviorSimplification");
  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam)
      throws IOException {

    // Remove typecasts
    viam.isas()
        .flatMap(isa -> isa.instructions().stream())
        .forEach(instruction -> {
          instruction.behavior().getNodes(TypeCastNode.class)
              .forEach(typeCastNode -> typeCastNode.replaceAndDelete(typeCastNode.value()));
        });

    return null;
  }
}
