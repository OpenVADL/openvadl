package vadl.lcb.passes.llvmLowering.tablegen.model;

import java.util.List;
import vadl.viam.graph.Graph;

/**
 * This is tablegen pattern defines a pseudo instruction expansion.
 */
public class TableGenPseudoInstExpansionPattern extends TableGenPattern {

  private final boolean isCall;
  private final List<TableGenInstructionOperand> outputs;
  private final List<TableGenInstructionOperand> inputs;
  private final Graph machine;

  public TableGenPseudoInstExpansionPattern(Graph selector, Graph machine,
                                            boolean isCall,
                                            List<TableGenInstructionOperand> inputs,
                                            List<TableGenInstructionOperand> outputs) {
    super(selector);
    this.isCall = isCall;
    this.machine = machine;
    this.inputs = inputs;
    this.outputs = outputs;
  }

  /**
   * Copy the {@code selector} and {@link #machine} and create new object.
   */
  @Override
  public TableGenPattern copy() {
    return new TableGenPseudoInstExpansionPattern(selector.copy(), machine.copy(), isCall, inputs,
        outputs);
  }

  public Graph machine() {
    return machine;
  }
}
