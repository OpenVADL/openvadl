package vadl.lcb.passes.llvmLowering.strategies.impl;

import static vadl.lcb.passes.isaMatching.InstructionLabel.BEQ;
import static vadl.lcb.passes.isaMatching.InstructionLabel.BGEQ;
import static vadl.lcb.passes.isaMatching.InstructionLabel.BGTH;
import static vadl.lcb.passes.isaMatching.InstructionLabel.BLEQ;
import static vadl.lcb.passes.isaMatching.InstructionLabel.BLTH;
import static vadl.lcb.passes.isaMatching.InstructionLabel.BNEQ;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.strategies.LlvmLoweringStrategy;
import vadl.lcb.passes.llvmLowering.visitors.ReplaceWithLlvmSDNodesVisitor;
import vadl.lcb.passes.llvmLowering.visitors.ReplaceWithLlvmSDNodesWithControlFlowVisitor;
import vadl.lcb.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.visitors.LcbGraphNodeVisitor;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * Lowering conditional branch instructions into TableGen patterns.
 */
public class LlvmLoweringConditionalBranchesStrategyImpl extends LlvmLoweringStrategy {
  @Override
  protected Set<InstructionLabel> getSupportedInstructionLabels() {
    return Set.of(BEQ, BGEQ, BNEQ, BLEQ, BLTH, BGTH);
  }

  @Override
  protected LcbGraphNodeVisitor getVisitorForPatternSelectorLowering() {
    // Branch instructions contain if conditionals.
    // The normal visitor denies those. But "xxxWithControlFlowVisitor" we are allowing
    // these instructions for conditional branches.
    return new ReplaceWithLlvmSDNodesWithControlFlowVisitor();
  }

  @Override
  public Optional<LlvmLoweringPass.LlvmLoweringIntermediateResult> lower(
      HashMap<InstructionLabel, List<Instruction>> supportedInstructions,
      Instruction instruction,
      InstructionLabel instructionLabel,
      Graph uninlinedBehavior) {

    var visitor = getVisitorForPatternSelectorLowering();

    for (var node : uninlinedBehavior.getNodes().toList()) {
      visitor.visit(node);
    }

    if (instructionLabel == InstructionLabel.BEQ) {
      return Optional.of(handleBeq(instruction, uninlinedBehavior));
    }

    return Optional.empty();
  }

  private LlvmLoweringPass.LlvmLoweringIntermediateResult handleBeq(
      Instruction instruction, Graph uninlinedBehavior) {
    var inputOperands = getTableGenInputOperands(uninlinedBehavior);
    var outputOperands = getTableGenOutputOperands(uninlinedBehavior);

    var copy = uninlinedBehavior.copy();
    copy.deinitializeNodes();
    var writes = copy.getNodes(WriteResourceNode.class).toList();

    return new LlvmLoweringPass.LlvmLoweringIntermediateResult(
        copy,
        inputOperands,
        outputOperands,
        generatePatterns(instruction, inputOperands, writes)
    );
  }

  @Override
  protected List<LlvmLoweringPass.LlvmLoweringTableGenPattern> generatePatternVariations(
      HashMap<InstructionLabel, List<Instruction>> supportedInstructions,
      InstructionLabel instructionLabel, Graph copy, List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<LlvmLoweringPass.LlvmLoweringTableGenPattern> patterns) {


    return Collections.emptyList();
  }

  /*

  @Override
  protected List<LlvmLoweringPass.LlvmLoweringTableGenPattern> generatePatterns(
      Instruction instruction, List<TableGenInstructionOperand> inputOperands,
      List<WriteResourceNode> sideEffectNodes) {

    return List.of(
        new LlvmLoweringPass.LlvmLoweringTableGenPattern()
    );
  }
   */
}
