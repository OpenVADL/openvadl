package vadl.viam.passes;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.Resource;
import vadl.viam.Specification;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * This pass resolves resources accesses of all {@link Instruction}s in the
 * {@link vadl.viam.InstructionSetArchitecture}.
 * All optimizations that might influence the result must be done beforehand
 * e.g. dead code elimination.
 *
 * <p>It uses the {@link Instruction#setReadResources(Set)} and
 * {@link Instruction#setWrittenResources(Set)} to set the results.
 */
public class InstructionResourceAccessAnalysisPass extends Pass {

  public InstructionResourceAccessAnalysisPass(
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
    viam.isa().ifPresent(isa -> isa
        .ownInstructions()
        .forEach(InstructionResourceAccessAnalysisPass::resolveAccesses));
    return null;
  }

  private static void resolveAccesses(Instruction instr) {
    var reads = new HashSet<Resource>();
    var writes = new HashSet<Resource>();

    instr.behavior()
        .getNodes(Set.of(ReadResourceNode.class, WriteResourceNode.class))
        .forEach(node -> {
          if (node instanceof ReadResourceNode readNode) {
            reads.add(readNode.resourceDefinition());
          } else {
            var writeNode = (WriteResourceNode) node;
            writes.add(writeNode.resourceDefinition());
          }
        });

    instr.setReadResources(reads);
    instr.setWrittenResources(writes);
  }
}

