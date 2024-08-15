package vadl.lcb.passes.llvmLowering.strategies.impl;

import static vadl.lcb.passes.isaMatching.InstructionLabel.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.ReplaceWithLlvmSDNodesVisitor;
import vadl.lcb.passes.llvmLowering.strategies.LlvmLoweringStrategy;
import vadl.lcb.tablegen.model.TableGenInstruction;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Lowers arithmetic and logic instructions into {@link TableGenInstruction}.
 */
public class LlvmLoweringArithmeticAndLogicStrategyImpl implements LlvmLoweringStrategy {

  private static final Logger logger = LoggerFactory.getLogger(
      LlvmLoweringArithmeticAndLogicStrategyImpl.class);

  private final Set<InstructionLabel> supported = Set.of(ADD_32,
      ADD_64, ADDI_32, ADDI_64, AND, OR, SUB, MUL, SUBB, SUBC, SDIV, UDIV, SMOD, UMOD, XOR, LT
  );

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
  public Optional<LlvmLoweringPass.LlvmLoweringIntermediateResult> lower(
      Identifier instructionIdentifier, Graph behavior) {
    var visitor = new ReplaceWithLlvmSDNodesVisitor();
    var copy = behavior.copy();
    var nodes = copy.getNodes().toList();

    if (!checkIfNoControlFlow(copy) && !checkIfNotAllowedDataflowNodes(copy)) {
      logger.atWarn().log("Instruction '{}' is not lowerable and will be skipped",
          instructionIdentifier.toString());
      return Optional.empty();
    }

    // Continue with lowering of nodes
    for (var node : nodes) {
      visitor.visit(node);

      if (!visitor.isPatternLowerable()) {
        logger.atWarn().log("Instruction '{}' is not lowerable and wil be skipped",
            instructionIdentifier.toString());
        break;
      }
    }

    var inputOperands = LlvmLoweringStrategy.getTableGenInputOperands(copy);
    var outputOperands = LlvmLoweringStrategy.getTableGenOutputOperands(copy);

    copy.deinitializeNodes();

    if (visitor.isPatternLowerable()) {
      var patterns = generatePatterns(copy.getNodes(WriteResourceNode.class).toList());
      return Optional.of(new LlvmLoweringPass.LlvmLoweringIntermediateResult(copy,
          inputOperands, outputOperands, patterns));
    }

    logger.atWarn().log("Instruction '{}' is not lowerable", instructionIdentifier.toString());
    return Optional.empty();
  }

  private List<Graph> generatePatterns(List<WriteResourceNode> sideEffectNodes) {
    ArrayList<Graph> patterns = new ArrayList<>();

    sideEffectNodes.forEach(sideEffectNode -> {
      var graph = new Graph(sideEffectNode.id().toString() + ".lowering");
      var root = sideEffectNode.value();
      root.clearUsages();
      graph.addWithInputs(root);
      patterns.add(graph);
    });

    return patterns;
  }
}
