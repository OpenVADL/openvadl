package vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand;

import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameter;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameterTypeAndName;
import vadl.viam.Format;
import vadl.viam.Parameter;
import vadl.viam.RegisterFile;
import vadl.viam.graph.dependency.FuncParamNode;
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

  public TableGenInstructionIndexedRegisterFileOperand(ReadRegFileNode node, FuncParamNode address) {
    super(node, new TableGenParameterTypeAndName(node.registerFile().simpleName(),
        address.parameter().identifier.simpleName()));
    this.registerFile = node.registerFile();
  }

  public TableGenInstructionIndexedRegisterFileOperand(WriteRegFileNode node, FuncParamNode address) {
    super(node, new TableGenParameterTypeAndName(node.registerFile().simpleName(),
        address.parameter().identifier.simpleName()));
    this.registerFile = node.registerFile();
  }

  public RegisterFile registerFile() {
    return registerFile;
  }
}
