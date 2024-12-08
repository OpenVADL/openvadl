package vadl.lcb.passes.llvmLowering.tablegen.model;

import java.util.List;
import vadl.viam.graph.Graph;

/**
 * This is tablegen pattern defines a pseudo instruction expansion.
 */
public class TableGenPseudoInstExpansionPattern extends TableGenPattern {

  private final String name;
  private final boolean isCall;
  private final List<TableGenInstructionOperand> outputs;
  private final List<TableGenInstructionOperand> inputs;
  private final Graph machine;

  public TableGenPseudoInstExpansionPattern(String name,
                                            Graph selector,
                                            Graph machine,
                                            boolean isCall,
                                            List<TableGenInstructionOperand> inputs,
                                            List<TableGenInstructionOperand> outputs) {
    super(selector);
    this.name = name;
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
    return new TableGenPseudoInstExpansionPattern(name, selector.copy(), machine.copy(), isCall,
        inputs,
        outputs);
  }

  public Graph machine() {
    return machine;
  }

  public String name() {
    return name;
  }

  public boolean isCall() {
    return isCall;
  }

  public List<TableGenInstructionOperand> outputs() {
    return outputs;
  }

  public List<TableGenInstructionOperand> inputs() {
    return inputs;
  }
}
