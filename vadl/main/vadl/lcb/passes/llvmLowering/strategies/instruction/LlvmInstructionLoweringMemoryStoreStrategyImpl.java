package vadl.lcb.passes.llvmLowering.strategies.instruction;

import static vadl.viam.ViamError.ensure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import vadl.lcb.passes.isaMatching.InstructionLabel;
import vadl.lcb.passes.llvmLowering.model.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.model.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.model.LlvmStoreSD;
import vadl.lcb.passes.llvmLowering.model.LlvmTruncStore;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.viam.Instruction;
import vadl.viam.Memory;
import vadl.viam.Register;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.passes.functionInliner.UninlinedGraph;

/**
 * Lowers instructions which can store into memory.
 */
public class LlvmInstructionLoweringMemoryStoreStrategyImpl
    extends LlvmInstructionLoweringFrameIndexHelper {

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
    return replaceRegisterWithFrameIndex(patterns);
  }

  /**
   * Instructions in {@link InstructionLabel#STORE_MEM} write from a {@link Register} into
   * {@link Memory}. However, LLVM has a special selection dag node for frame indexes.
   * Function's variables are placed on the stack and will be accessed relative to a frame pointer.
   * LLVM has for the lowering a frame index leaf node which requires additional patterns.
   * The goal of this method is to replace a {@link Register} with {@link LlvmFrameIndexSD}
   * which has a LLVM's {@code ComplexPattern} hardcoded.
   */
  private List<TableGenPattern> replaceRegisterWithFrameIndex(List<TableGenPattern> patterns) {
    var alternativePatterns = new ArrayList<TableGenPattern>();

    for (var pattern : patterns) {
      var selector = pattern.selector().copy();
      var machine = pattern.machine().copy();

      // We are only interested in the `address` subtree of a memory store (or truncstore)
      // because the value register should remain unchanged.
      // Afterward, we get all the children of the `WriteResource` and only filter for
      // `LlvmReadRegFileNode` because we wil only change registers.
      var affectedNodes = selector.getNodes(Set.of(LlvmTruncStore.class, LlvmStoreSD.class))
          .map(x -> (WriteResourceNode) x)
          .filter(WriteResourceNode::hasAddress)
          .flatMap(x -> {
            var inputs = new ArrayList<Node>();
            var address = x.address();
            ensure(address != null, "address must not be null");
            address.collectInputsWithChildren(inputs);
            return inputs.stream();
          })
          .filter(x -> x instanceof LlvmReadRegFileNode)
          .map(x -> (LlvmReadRegFileNode) x)
          .toList();

      alternativePatterns.add(
          super.replaceRegisterWithFrameIndex(selector, machine, affectedNodes));
    }

    return alternativePatterns;
  }
}
