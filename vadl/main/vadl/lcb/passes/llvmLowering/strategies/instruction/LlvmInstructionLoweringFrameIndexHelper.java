package vadl.lcb.passes.llvmLowering.strategies.instruction;

import static vadl.viam.ViamError.ensureNonNull;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionParameterNode;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmReadRegFileNode;
import vadl.lcb.passes.llvmLowering.strategies.LlvmInstructionLoweringStrategy;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionFrameRegisterOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameterTypeAndName;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * Common superclass for {@link LlvmInstructionLoweringMemoryLoadStrategyImpl} and
 * {@link LlvmInstructionLoweringMemoryStoreStrategyImpl}.
 */
public abstract class LlvmInstructionLoweringFrameIndexHelper
    extends LlvmInstructionLoweringStrategy {
  public LlvmInstructionLoweringFrameIndexHelper(
      ValueType architectureType) {
    super(architectureType);
  }

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
    BiFunction<LcbMachineInstructionParameterNode,
        TableGenParameterTypeAndName,
        TableGenInstructionOperand>
        machineInstructionTransformation = (machineInstructionParameterNode,
                                            affectedParameterIdentity) -> {

      var node = ensureNonNull(machineInstructionParameterNode.instructionOperand().origin(),
          "origin must exist");
      if (node instanceof LlvmReadRegFileNode llvmReadRegFileNode
          && llvmReadRegFileNode.address() instanceof FieldRefNode fieldRefNode) {
        return new TableGenInstructionFrameRegisterOperand(llvmReadRegFileNode, fieldRefNode);
      } else if (node instanceof LlvmReadRegFileNode llvmReadRegFileNode
          && llvmReadRegFileNode.address() instanceof FuncParamNode funcParamNode) {
        return new TableGenInstructionFrameRegisterOperand(llvmReadRegFileNode, funcParamNode);
      } else {
        throw Diagnostic.error("Node type is not supported to be replaced", node.sourceLocation())
            .build();
      }
    };

    replaceNodeByParameterIdentity(affectedNodes,
        machine,
        selectorTransformation,
        machineInstructionTransformation);

    return new TableGenSelectionWithOutputPattern(selector, machine);
  }
}
