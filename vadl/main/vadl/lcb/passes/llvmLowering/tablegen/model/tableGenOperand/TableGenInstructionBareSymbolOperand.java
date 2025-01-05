package vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand;

import org.jetbrains.annotations.Nullable;
import vadl.viam.graph.Node;

/**
 * Represents a bare symbol operand in TableGen.
 */
public class TableGenInstructionBareSymbolOperand extends TableGenInstructionOperand {
  public TableGenInstructionBareSymbolOperand(
      @Nullable Node origin, String type, String name) {
    super(origin, type, name);
  }
}
