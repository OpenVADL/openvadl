package vadl.lcb.passes.llvmLowering.strategies.instruction;

import static vadl.lcb.passes.isaMatching.InstructionLabel.ADDI_32;
import static vadl.lcb.passes.isaMatching.InstructionLabel.ADDI_64;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.viam.Instruction;
import vadl.viam.graph.Graph;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * Lowers add with immediate into {@link TableGenInstruction} and additionally,
 * creates alternative patterns with {@link LlvmFrameIndexSD}.
 */
public class LlvmInstructionLoweringAddImmediateStrategyImpl
    extends LlvmInstructionLoweringFrameIndexHelper {
  private final Set<InstructionLabel> supported = Set.of(ADDI_32, ADDI_64);

  public LlvmInstructionLoweringAddImmediateStrategyImpl(
      ValueType architectureType) {
    super(architectureType);
  }

  @Override
  protected Set<InstructionLabel> getSupportedInstructionLabels() {
    return supported;
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      Instruction instruction,
      Map<InstructionLabel, List<Instruction>> supportedInstructions,
      Graph behavior,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns) {
    var alternativePatterns = new ArrayList<TableGenPattern>();

    // We are only interested in the pattern with selector and machine pattern.
    patterns.stream()
        .filter(p -> p instanceof TableGenSelectionWithOutputPattern)
        .map(p -> (TableGenSelectionWithOutputPattern) p)
        .forEach(pattern -> {
          var selector = pattern.selector().copy();
          var machine = pattern.machine().copy();

          var affectedNodes = selector.getNodes(LlvmReadRegFileNode.class).toList();
          alternativePatterns.add(
              super.replaceRegisterWithFrameIndex(selector, machine, affectedNodes));
        });

    return alternativePatterns;
  }
}
