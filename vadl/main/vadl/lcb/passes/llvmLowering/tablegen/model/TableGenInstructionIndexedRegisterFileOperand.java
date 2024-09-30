package vadl.lcb.passes.llvmLowering.tablegen.model;

import vadl.viam.Format;
import vadl.viam.Parameter;
import vadl.viam.RegisterFile;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.WriteRegFileNode;

/**
 * Indicates that the operand is a {@link RegisterFile} when the address is *not*
 * a {@link Format.Field} but is indexed by a function. This is useful when we have to generate
 * tablegen instruction operands from operands.
 * In the example below we have {@code rd} and {@code rs1} which are both indexes and have no
 * {@link Format.Field}.
 * <code>
 * pseudo instruction MOV( rd : Index, rs1 : Index ) =
 * {
 * ADDI{ rd = rd, rs1 = rs1, imm = 0 as Bits12 }
 * }
 * </code>
 */
public class TableGenInstructionIndexedRegisterFileOperand extends TableGenInstructionOperand {
  private final RegisterFile registerFile;
  private final Parameter parameter;

  /**
   * Constructor.
   */
  public TableGenInstructionIndexedRegisterFileOperand(ParameterIdentity identity,
                                                       ReadRegFileNode node,
                                                       Parameter parameter) {
    super(node, identity);
    this.registerFile = node.registerFile();
    this.parameter = parameter;
  }

  /**
   * Constructor.
   */
  public TableGenInstructionIndexedRegisterFileOperand(ParameterIdentity identity,
                                                       WriteRegFileNode node,
                                                       Parameter parameter) {
    super(node, identity);
    this.registerFile = node.registerFile();
    this.parameter = parameter;
  }

  public RegisterFile registerFile() {
    return registerFile;
  }

  public Parameter formatField() {
    return parameter;
  }
}
