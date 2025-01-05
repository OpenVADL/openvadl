package vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand;

import org.jetbrains.annotations.Nullable;
import vadl.viam.graph.Node;

/**
 * Represents a single register in TableGen. This register might not be specified in the
 * specification because it was computed.
 */
public class TableGenInstructionRegisterOperand extends TableGenInstructionOperand {
  public TableGenInstructionRegisterOperand(
      @Nullable Node origin, String name) {
    super(origin, "", name);
  }
}
