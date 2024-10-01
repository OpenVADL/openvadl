package vadl.lcb.passes.llvmLowering.tablegen.model;

import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.RegisterFile;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.WriteRegFileNode;

/**
 * Indicates that the operand is a {@link RegisterFile} when the address is *not*
 * a {@link Format.Field} but is indexed by a constant. This is useful when we have to generate
 * tablegen instruction operands from operands.
 * In the example below we have {@code rd} and {@code rs1} which are both indexes and have no
 * {@link Format.Field}.
 * <code>
 * pseudo instruction MOV =
 * {
 * ADDI{ rd = 0, rs1 = 1, imm = 0 as Bits12 }
 * }
 * </code>
 */
public class TableGenInstructionConstantIndexedRegisterFileOperand
    extends TableGenInstructionOperand {
  private final RegisterFile registerFile;
  private final Constant constant;

  /**
   * Constructor.
   */
  public TableGenInstructionConstantIndexedRegisterFileOperand(ParameterIdentity identity,
                                                               ReadRegFileNode node,
                                                               Constant constant) {
    super(node, identity);
    this.registerFile = node.registerFile();
    this.constant = constant;
  }

  /**
   * Constructor.
   */
  public TableGenInstructionConstantIndexedRegisterFileOperand(ParameterIdentity identity,
                                                               WriteRegFileNode node,
                                                               Constant constant) {
    super(node, identity);
    this.registerFile = node.registerFile();
    this.constant = constant;
  }

  public RegisterFile registerFile() {
    return registerFile;
  }

  public Constant constant() {
    return constant;
  }
}
