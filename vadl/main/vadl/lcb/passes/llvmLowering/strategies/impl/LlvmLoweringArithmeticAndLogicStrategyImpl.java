package vadl.lcb.passes.llvmLowering.strategies.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.ReplaceWithLlvmSDNodesVisitor;
import vadl.lcb.passes.llvmLowering.strategies.LlvmLoweringStrategy;
import vadl.lcb.tablegen.model.TableGenInstruction;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Lowers arithmetic and logic instructions into {@link TableGenInstruction}.
 */
public class LlvmLoweringArithmeticAndLogicStrategyImpl implements LlvmLoweringStrategy {

  private static final Logger logger = LoggerFactory.getLogger(
      LlvmLoweringArithmeticAndLogicStrategyImpl.class);

  private final Set<InstructionLabel> supported = Set.of(InstructionLabel.ADD_32,
      InstructionLabel.ADD_64, InstructionLabel.ADDI_32, InstructionLabel.ADDI_64);

  @Override
  public boolean isApplicable(Map<Instruction, InstructionLabel> matching,
                              Instruction instruction) {
    var key = matching.get(instruction);

    if (key == null) {
      return false;
    }

    return supported.contains(key);
  }

  @Override
  public Optional<LlvmLoweringPass.LlvmLoweringIntermediateResult> lower(Instruction instruction) {
    var visitor = new ReplaceWithLlvmSDNodesVisitor();
    var copy = instruction.behavior().copy();
    copy.deinitialize_nodes();
    var nodes = copy.getNodes().toList();

    if (!checkIfNoControlFlow(copy) && !checkIfNotAllowedDataflowNodes(copy)) {
      logger.atWarn().log("Instruction '{}' is not lowerable and will be skipped",
          instruction.identifier.toString());
      return Optional.empty();
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
      var patterns = generatePatterns(copy.getNodes(WriteResourceNode.class).toList());
      return Optional.of(new LlvmLoweringPass.LlvmLoweringIntermediateResult(copy,
          inputOperands, outputOperands, patterns));
    }

    logger.atWarn().log("Instruction '{}' is not lowerable", instruction.identifier.toString());
    return Optional.empty();
  }

  private List<Graph> generatePatterns(List<WriteResourceNode> sideEffectNodes) {
    ArrayList<Graph> patterns = new ArrayList<>();

    sideEffectNodes.forEach(sideEffectNode -> {
      var graph = new Graph(sideEffectNode.id().toString() + ".lowering");
      graph.addWithInputs(sideEffectNode.value());
      patterns.add(graph);
    });

    return patterns;
  }
}
