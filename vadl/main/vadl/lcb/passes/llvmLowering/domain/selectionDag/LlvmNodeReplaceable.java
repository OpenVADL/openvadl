package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.tableGenParameter.TableGenParameter;

/**
 * Indicates that the node has a {@link TableGenParameter} which
 * makes it replaceable for selector and machine graphs.
 * This is useful when already a {@link TableGenPattern} exists,
 * and you want to replace a single node in it to create new pattern.
 * This has to be changed in both graphs.
 */
public interface LlvmNodeReplaceable {
  /**
   * Returns the {@link TableGenInstructionOperand} of the node.
   */
  TableGenInstructionOperand operand();
}
