package vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand;

import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameterTypeAndName;
import vadl.viam.Register;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;

/**
 * Indicates that the operand is a {@link Register} which is the frame pointer.
 */
public class TableGenInstructionFrameRegisterOperand extends TableGenInstructionOperand {

  /**
   * Constructor.
   */
  public TableGenInstructionFrameRegisterOperand(Node node,
                                                 FieldRefNode address) {
    // Note that `node` has the type `Node` and not `LlvmFrameIndex` because
    // the machine pattern requires that the node remains a register class file operand.
    super(node, new TableGenParameterTypeAndName(LlvmFrameIndexSD.NAME,
        address.formatField().identifier.simpleName()));
  }

  /**
   * Constructor.
   */
  public TableGenInstructionFrameRegisterOperand(Node node,
                                                 FuncParamNode address) {
    // Note that `node` has the type `Node` and not `LlvmFrameIndex` because
    // the machine pattern requires that the node remains a register class file operand.
    super(node, new TableGenParameterTypeAndName(LlvmFrameIndexSD.NAME,
        address.parameter().identifier.simpleName()));
  }

  @Override
  public String render() {
    var paramIdentity = (TableGenParameterTypeAndName) parameter();
    return "AddrFI:$" + paramIdentity.name();
  }
}
