package vadl.lcb.passes.llvm_lowering;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.EndNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * Lowers the {@link Instruction#behavior()} into a LLVM tree pattern for tablegen.
 */
public class LlvmLoweringPass extends Pass {
  private static final Logger logger = LoggerFactory.getLogger(LlvmLoweringPass.class);

  @Override
  public PassName getName() {
    return new PassName("LlvmLoweringPass");
  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam)
      throws IOException {
    Map<Instruction, Graph> llvmPattern = new IdentityHashMap<>();

    viam.isas()
        .flatMap(isa -> isa.instructions().stream())
        .forEach(instruction -> {
          var visitor = new ReplaceWithLlvmSDNodesVisitor();
          var copy = instruction.behavior().copy();
          var nodes = copy.getNodes().toList();

          if (!checkIfNoControlFlow(copy) && !checkIfNotAllowedDataflowNodes(copy)) {
            logger.atWarn().log("Instruction '{}' is not lowerable and will be skipped",
                instruction.identifier.toString());
            return;
          }

          // Continue with lowering of nodes
          for (var node : nodes) {
            visitor.visit(node);

            if (!visitor.isPatternLowerable()) {
              logger.atWarn().log("Instruction '{}' is not lowerable and wil be skipped",
                  instruction.identifier.toString());
              break;
            }
          }

          if (visitor.isPatternLowerable()) {
            llvmPattern.put(instruction, copy);
          }
        });

    return llvmPattern;
  }

  /**
   * LLvm's TableGen cannot work with control flow. So if statements and other constructs are not
   * supported.
   *
   * @return {@code true} if the {@link Graph} is lowerable.
   */
  private boolean checkIfNoControlFlow(Graph behavior) {
    return behavior.getNodes(ControlNode.class)
        .allMatch(x -> x instanceof AbstractBeginNode || x instanceof EndNode); // exceptions
  }

  /**
   * Some dataflow nodes are not lowerable. This function checks whether the {@code behavior}
   * contains these.
   *
   * @return {@code true} if the {@link Graph} is lowerable.
   */
  private boolean checkIfNotAllowedDataflowNodes(Graph behavior) {
    return behavior.getNodes(DependencyNode.class)
        .noneMatch(x -> x instanceof FuncParamNode);
  }
}
