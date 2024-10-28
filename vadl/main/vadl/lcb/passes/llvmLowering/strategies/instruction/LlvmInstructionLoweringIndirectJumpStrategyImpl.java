package vadl.lcb.passes.llvmLowering.strategies.instruction;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import vadl.lcb.codegen.model.llvm.ValueType;
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
  public LlvmInstructionLoweringIndirectJumpStrategyImpl(
      ValueType architectureType) {
    super(architectureType);
  }

  @Override
  protected Set<InstructionLabel> getSupportedInstructionLabels() {
    return Set.of(InstructionLabel.JALR);
  }

  @Override
  protected Optional<LlvmLoweringRecord> lowerInstruction(
      Map<InstructionLabel, List<Instruction>> supportedInstructions,
      Instruction instruction,
      Graph unmodifiedBehavior) {
    var copy = unmodifiedBehavior.copy();
    var visitor = getVisitorForPatternSelectorLowering();

    for (var node : copy.getNodes(AbstractEndNode.class).toList()) {
      visitor.visit(node);
    }

    var inputOperands = getTableGenInputOperands(copy);
    var outputOperands = getTableGenOutputOperands(copy);

    // If a TableGen record has no input or output operands,
    // and no registers as def or use then it will throw an error.
    // Therefore, when input and output operands are empty then do not filter any
    // registers.
    var filterRegistersWithConstraints = inputOperands.isEmpty() && outputOperands.isEmpty();
    var uses = getRegisterUses(copy, filterRegistersWithConstraints);
    var defs = getRegisterDefs(copy, filterRegistersWithConstraints);

    return Optional.of(new LlvmLoweringRecord(
        copy,
        inputOperands,
        outputOperands,
        LlvmLoweringPass.Flags.empty(),
        Collections.emptyList(), // TODO: currently do not generate indirect call
        uses,
        defs
    ));
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      Instruction instruction,
      Map<InstructionLabel, List<Instruction>> supportedInstructions,
      Graph behavior,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns) {
    return Collections.emptyList();
  }
}
