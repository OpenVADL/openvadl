package vadl.lcb.passes.llvmLowering;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.tablegen.model.TableGenInstructionOperand;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.EndNode;
import vadl.viam.graph.dependency.DependencyNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteRegFileNode;

/**
 * Lowers the {@link Instruction#behavior()} into a LLVM tree pattern for tablegen.
 * This pass only lowers which have been recognized by {@link InstructionLabel}. Also
 * this pass only lowers arithmetic and logic instructions.
 */
public class LlvmLoweringKnownArithmeticLogicLabelsPass extends LlvmLoweringPass {
  private static final Logger logger = LoggerFactory.getLogger(
      LlvmLoweringKnownArithmeticLogicLabelsPass.class);

  private final Set<InstructionLabel> supported = Set.of(InstructionLabel.ADD_32,
      InstructionLabel.ADD_64, InstructionLabel.ADDI_32, InstructionLabel.ADDI_64);

  public record LlvmLoweringIntermediateResult(Graph behavior,
                                               List<TableGenInstructionOperand> inputs,
                                               List<TableGenInstructionOperand> outputs) {

  }

  @Override
  public PassName getName() {
    return new PassName("LlvmLoweringPass");
  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam)
      throws IOException {
    Map<Instruction, LlvmLoweringIntermediateResult> llvmPatterns = new IdentityHashMap<>();
    var isaMatched =
        (HashMap<InstructionLabel, List<Instruction>>) passResults.get(new PassKey("IsaMatchingPass"));
    ensure(isaMatched != null, "Cannot find pass results from isaMatched");

    supported.stream().map(isaMatched::get)
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .forEach(instruction-> {
          var visitor = new ReplaceWithLlvmSDNodesVisitor();
          var copy = instruction.behavior().copy();
          var nodes = copy.getNodes().toList();

          if (!checkIfNoControlFlow(copy) && !checkIfNotAllowedDataflowNodes(copy)) {
            logger.atWarn().log("Instruction '{}' is not lowerable and will be skipped",
                instruction.identifier.toString());
            return;
          }

          var inputOperands = getTableGenInputOperands(copy);
          var outputOperands = getTableGenOutputOperands(copy);

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
            llvmPatterns.put(instruction,
                new LlvmLoweringIntermediateResult(copy, inputOperands, outputOperands));
          }
        });

    return llvmPatterns;
  }


}
