package vadl.lcb.passes.llvmLowering.strategies.instruction;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * Generates the {@link LlvmLoweringRecord} for {@link MachineInstructionLabel#JALR}
 * instruction.
 */
public class LlvmInstructionLoweringIndirectJumpStrategyImpl
    extends LlvmInstructionLoweringStrategy {
  public LlvmInstructionLoweringIndirectJumpStrategyImpl(
      ValueType architectureType) {
    super(architectureType);
  }

  @Override
  protected Set<MachineInstructionLabel> getSupportedInstructionLabels() {
    return Set.of(MachineInstructionLabel.JALR);
  }

  @Override
  protected Optional<LlvmLoweringRecord> lowerInstruction(
      Map<MachineInstructionLabel, List<Instruction>> labelledMachineInstructions,
      Instruction instruction,
      Graph unmodifiedBehavior,
      @Nullable List<UninlinedGraph> unmodifiedAdditionalBehaviors) {
    var copy = unmodifiedBehavior.copy();
    var visitor = replacementHooksWithDefaultFieldAccessReplacement();

    for (var node : copy.getNodes(SideEffectNode.class).toList()) {
      visitReplacementHooks(visitor, node);
    }

    var outputOperands = getTableGenOutputOperands(copy);
    var inputOperands = getTableGenInputOperands(outputOperands, copy);

    var uses = getRegisterUses(copy, inputOperands, outputOperands);
    var defs = getRegisterDefs(copy, inputOperands, outputOperands);

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
  protected List<GraphVisitor.NodeApplier<? extends Node, ? extends Node>> replacementHooks() {
    return replacementHooksWithDefaultFieldAccessReplacement();
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      Instruction instruction,
      Map<MachineInstructionLabel, List<Instruction>> supportedInstructions,
      Graph behavior,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns) {
    return Collections.emptyList();
  }
}
