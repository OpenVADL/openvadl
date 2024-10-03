package vadl.lcb.passes.llvmLowering.strategies.instruction;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * Generates the {@link LlvmLoweringRecord} for {@link InstructionLabel#JALR}
 * instruction.
 */
public class LlvmInstructionLoweringIndirectJumpStrategyImpl
    extends LlvmInstructionLoweringStrategy {
  @Override
  protected Set<InstructionLabel> getSupportedInstructionLabels() {
    return Set.of(InstructionLabel.JALR);
  }

  @Override
  protected Optional<LlvmLoweringRecord> lowerInstruction(
      Map<InstructionLabel, List<Instruction>> supportedInstructions,
      Instruction instruction,
      InstructionLabel instructionLabel,
      Graph unmodifiedBehavior) {
    var copy = unmodifiedBehavior.copy();
    var visitor = getVisitorForPatternSelectorLowering();

    for (var node : copy.getNodes(AbstractEndNode.class).toList()) {
      visitor.visit(node);
    }

    return Optional.of(new LlvmLoweringRecord(
        copy,
        getTableGenInputOperands(copy),
        getTableGenOutputOperands(copy),
        LlvmLoweringPass.Flags.empty(),
        Collections.emptyList(), // TODO: currently do not generate indirect call
        getRegisterUses(copy),
        getRegisterDefs(copy)
    ));
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      Instruction instruction,
      Map<InstructionLabel, List<Instruction>> supportedInstructions,
      InstructionLabel instructionLabel,
      Graph behavior,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns) {
    return Collections.emptyList();
  }
}
