package vadl.lcb.passes.llvmLowering.tablegen.model;

import java.util.List;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.RegisterRef;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.viam.PseudoInstruction;

/**
 * Represents a record in tablegen for {@link PseudoInstruction}.
 */
public class TableGenPseudoInstruction extends TableGenInstruction {
  public TableGenPseudoInstruction(String name, String namespace,
                                   LlvmLoweringPass.Flags flags,
                                   List<TableGenInstructionOperand> inOperands,
                                   List<TableGenInstructionOperand> outOperands,
                                   List<RegisterRef> uses,
                                   List<RegisterRef> defs,
                                   List<TableGenPattern> anonymousPatterns) {
    super(name, namespace, flags, inOperands, outOperands, uses, defs, anonymousPatterns);
  }
}
