package vadl.lcb.passes.llvmLowering.tablegen.model;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensurePresent;

import vadl.error.Diagnostic;
import vadl.lcb.codegen.model.llvm.ValueType;
import vadl.lcb.passes.llvmLowering.tablegen.model.parameterIdentity.NoParameterIdentity;
import vadl.utils.SourceLocation;
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
    ensure(!constantNode.constant().equals(value),
        "This is definitely wrong because index and constraint value are mismatched");
    this.constant = value;
  }

  public Constant constant() {
    return constant;
  }

  @Override
  public String render() {
    var llvmType = ValueType.from(constant.type());
    var unpackedLlvmType = ensurePresent(llvmType, () -> Diagnostic.error(
        "Constant value at given index has an invalid type which is not supported by llvm: "
            + constant.type(),
        origin != null ? origin.sourceLocation() : SourceLocation.INVALID_SOURCE_LOCATION));
    return "(" + unpackedLlvmType.getLlvmType() + " " + constant.asVal().intValue() + ")";
  }
}
