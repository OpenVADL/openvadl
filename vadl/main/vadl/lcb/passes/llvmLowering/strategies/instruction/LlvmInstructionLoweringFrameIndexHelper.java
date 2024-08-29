package vadl.lcb.passes.llvmLowering.strategies.instruction;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import vadl.lcb.passes.llvmLowering.model.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.model.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.model.MachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.ParameterIdentity;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionFrameRegisterOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;

/**
 * Common superclass for {@link LlvmInstructionLoweringMemoryLoadStrategyImpl} and
 * {@link LlvmInstructionLoweringMemoryStoreStrategyImpl}.
 */
public abstract class LlvmInstructionLoweringFrameIndexHelper
    extends LlvmInstructionLoweringStrategy {
  /**
   * Replaces {@link LlvmReadRegFileNode} with {@link LlvmFrameIndexSD} in the selector
   * and changes the same operands in the machine pattern to
   * {@link TableGenInstructionFrameRegisterOperand}. The new {@link TableGenPattern} will be
   * returned. Note that {@code selector} and {@code machine} must be copies from the original
   * pattern.
   */
  protected TableGenPattern replaceRegisterWithFrameIndex(
      Graph selector,
      Graph machine,
      List<LlvmReadRegFileNode> affectedNodes) {
    // Both functions specify how the graph should be changed.
    // Selector: replace the original node by `LlvmFrameIndexSD`
    Function<LlvmReadRegFileNode, Node> selectorTransformation = LlvmFrameIndexSD::new;
    // Machine: replace by frame register operand but the `affectedParameterIdentity`
    // should change the type. So `X:$rs1` should be `Addr:$rs1`.
    BiFunction<MachineInstructionParameterNode, ParameterIdentity, TableGenInstructionOperand>
        machineInstructionTransformation = (machineInstructionParameterNode,
                                            affectedParameterIdentity) ->
        new TableGenInstructionFrameRegisterOperand(
            affectedParameterIdentity.withType(LlvmFrameIndexSD.NAME),
            machineInstructionParameterNode.instructionOperand().origin()
        );

    replaceNodeByParameterIdentity(affectedNodes,
        machine,
        selectorTransformation,
        machineInstructionTransformation);

    return new TableGenPattern(selector, machine);
  }
}
