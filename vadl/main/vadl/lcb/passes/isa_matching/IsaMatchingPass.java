package vadl.lcb.passes.isa_matching;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;

/**
 * A {@link InstructionSetArchitecture} contains a {@link List} of {@link Instruction}.
 * One of the most important tasks of the LCB is to recognize the semantics of the machine
 * instruction and create a mapping from LLVM's SelectionDag to the machine instruction.
 * Most instructions in an instruction set architecture will be "simple" and
 * a lot of them are required in almost every instruction set architecture. The goal
 * of {@link IsaMatchingPass} to label instructions which can be recognized.
 * Why is this useful?
 * At some places, we need to create machine instructions by hand in LLVM, and we need to
 * know which instructions are supported by instruction set. This labelling makes it much
 * easier to search for these instructions.
 */
public class IsaMatchingPass extends Pass {
  @Override
  public PassName getName() {
    return new PassName("IsaMatchingPass");
  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam)
      throws IOException {
    return null;
  }
}
