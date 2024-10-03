package vadl.lcb.passes.llvmLowering.tablegen.model;

import vadl.lcb.passes.llvmLowering.tablegen.model.parameterIdentity.ParameterIdentity;
import vadl.viam.Register;
import vadl.viam.graph.Node;

/**
 * Indicates that the operand is a {@link Register} which is the frame pointer.
 */
public class TableGenInstructionFrameRegisterOperand extends TableGenInstructionOperand {

  /**
   * Contructor.
   */
  public TableGenInstructionFrameRegisterOperand(ParameterIdentity identity, Node node) {
    // Note that `node` has the type `Node` and not `LlvmFrameIndex` because
    // the machine pattern requires that the node remains a register class file operand.
    super(node, identity);
  }
}
