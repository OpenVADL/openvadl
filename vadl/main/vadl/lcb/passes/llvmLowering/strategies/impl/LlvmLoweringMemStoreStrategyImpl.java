package vadl.lcb.passes.llvmLowering.strategies.impl;

import static vadl.viam.ViamError.ensure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.llvmLowering.model.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.model.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.model.LlvmTruncStore;
import vadl.lcb.passes.llvmLowering.model.MachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.strategies.LlvmLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionFrameRegisterOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.viam.Instruction;
import vadl.viam.Memory;
import vadl.viam.Register;
import vadl.viam.graph.Node;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * Lowers instructions which can store into memory.
 */
public class LlvmLoweringMemStoreStrategyImpl extends LlvmLoweringStrategy {

  @Override
  protected Set<InstructionLabel> getSupportedInstructionLabels() {
    return Set.of(InstructionLabel.STORE_MEM);
  }

  @Override
  protected List<TableGenPattern> generatePatternVariations(
      Instruction instruction,
      Map<InstructionLabel, List<Instruction>> supportedInstructions,
      InstructionLabel instructionLabel,
      UninlinedGraph behavior,
      List<TableGenInstructionOperand> inputOperands,
      List<TableGenInstructionOperand> outputOperands,
      List<TableGenPattern> patterns) {
    return replaceRegisterWithFrameIndexForLlvmTruncNodes(patterns);
  }

  /**
   * Instructions in {@link InstructionLabel#STORE_MEM} write from a {@link Register} into
   * {@link Memory}. However, LLVM has a special selection dag node for frame indexes.
   * Function's variables are placed on the stack and will be accessed relative to a frame pointer.
   * LLVM has for the lowering a frame index leaf node which requires additional patterns.
   * The goal of this method is to replace a {@link Register} with {@link LlvmFrameIndexSD}
   * which has a LLVM's {@code ComplexPattern} hardcoded.
   */
  private List<TableGenPattern> replaceRegisterWithFrameIndexForLlvmTruncNodes(
      List<TableGenPattern> patterns) {
    var alternativePatterns = new ArrayList<TableGenPattern>();
    for (var pattern : patterns) {
      var selector = pattern.selector().copy();
      var machine = pattern.machine().copy();

      var affectedNodes = selector.getNodes(LlvmTruncStore.class).toList();
      for (var truncStore : affectedNodes) {
        if (truncStore.hasAddress()) {
          var address = truncStore.address();
          ensure(address != null, "address must not be null");
          var inputs = new ArrayList<Node>();
          address.collectInputsWithChildren(inputs);

          for (var node : inputs.stream()
              .filter(x -> x instanceof LlvmReadRegFileNode)
              .map(x -> (LlvmReadRegFileNode) x)
              .toList()) {
            // Replace every register with frame index (under the address subtree of truncstore).
            var selectorParameterIdentity = node.parameterIdentity();
            node.replaceAndDelete(new LlvmFrameIndexSD(node));

            // We have to adapt the machine pattern too. We find it by the
            // selectorParameterIdentity.
            var occurrencesInMachinePattern =
                machine.getNodes(MachineInstructionParameterNode.class)
                    .filter(
                        x -> x.instructionOperand()
                            .origin() instanceof LlvmReadRegFileNode regFileNode
                            && regFileNode.parameterIdentity().equals(selectorParameterIdentity))
                    .toList();
            for (var occurrence : occurrencesInMachinePattern) {
              // Create new operand with frame index.
              occurrence.setInstructionOperand(
                  new TableGenInstructionFrameRegisterOperand(selectorParameterIdentity.withType(
                      LlvmFrameIndexSD.NAME
                  ), occurrence.instructionOperand().origin()));
            }
          }
        }

        alternativePatterns.add(new TableGenPattern(selector, machine));
      }
    }

    return alternativePatterns;
  }
}
