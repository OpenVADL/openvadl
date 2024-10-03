package vadl.lcb.passes.llvmLowering.tablegen.model;

import static vadl.viam.ViamError.ensure;

import vadl.lcb.passes.llvmLowering.tablegen.model.parameterIdentity.NoParameterIdentity;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.RegisterFile;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.WriteRegFileNode;

/**
 * Indicates that the operand is a {@link Constant} index of a {@link RegisterFile}. It
 * can be only lowered when the register file at that constant is also a constant.
 */
public class TableGenConstantOperand extends TableGenInstructionOperand {
  private final Constant constant;

  /**
   * Constructor.
   */
  public TableGenConstantOperand(ConstantNode constantNode, Constant value) {
    super(constantNode, new NoParameterIdentity());
    ensure(constantNode.constant() != value,
        "This is definitely wrong because index and constraint value are mismatched");
    this.constant = value;
  }

  public Constant constant() {
    return constant;
  }
}
