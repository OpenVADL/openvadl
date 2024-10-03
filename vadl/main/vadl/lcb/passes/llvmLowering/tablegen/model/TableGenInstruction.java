package vadl.lcb.passes.llvmLowering.tablegen.model;

import java.util.List;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.domain.RegisterRef;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;

/**
 * Models an {@link Instruction} and {@link PseudoInstruction} for TableGen.
 */
public abstract class TableGenInstruction {
  private final String name;
  private final String namespace;
  private final List<TableGenPattern> anonymousPatterns;
  private final List<TableGenInstructionOperand> inOperands;
  private final List<TableGenInstructionOperand> outOperands;
  private final List<RegisterRef> uses;
  private final List<RegisterRef> defs;
  private final LlvmLoweringPass.Flags flags;


  /**
   * Constructor.
   */
  public TableGenInstruction(String name,
                             String namespace,
                             LlvmLoweringPass.Flags flags,
                             List<TableGenInstructionOperand> inOperands,
                             List<TableGenInstructionOperand> outOperands,
                             List<RegisterRef> uses,
                             List<RegisterRef> defs,
                             List<TableGenPattern> anonymousPatterns) {
    this.name = name;
    this.namespace = namespace;
    this.flags = flags;
    this.inOperands = inOperands;
    this.outOperands = outOperands;
    this.uses = uses;
    this.defs = defs;
    this.anonymousPatterns = anonymousPatterns;
  }

  public String getNamespace() {
    return namespace;
  }

  public List<TableGenPattern> getAnonymousPatterns() {
    return anonymousPatterns;
  }

  public List<RegisterRef> getUses() {
    return uses;
  }

  public List<RegisterRef> getDefs() {
    return defs;
  }

  public LlvmLoweringPass.Flags getFlags() {
    return flags;
  }

  public List<TableGenInstructionOperand> getInOperands() {
    return inOperands;
  }

  public List<TableGenInstructionOperand> getOutOperands() {
    return outOperands;
  }

  public String getName() {
    return name;
  }


}
