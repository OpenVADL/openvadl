package vadl.lcb.passes;

import vadl.lcb.passes.llvmLowering.domain.LlvmLoweringRecord;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.Instruction;

/**
 * Stores the {@link LlvmLoweringRecord} to the {@link Instruction}.
 */
public class TableGenInstructionCtx extends DefinitionExtension<Instruction> {
  private final LlvmLoweringRecord record;

  /**
   * Constructor.
   */
  public TableGenInstructionCtx(LlvmLoweringRecord record) {
    this.record = record;
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return Definition.class;
  }

  public LlvmLoweringRecord record() {
    return record;
  }
}
