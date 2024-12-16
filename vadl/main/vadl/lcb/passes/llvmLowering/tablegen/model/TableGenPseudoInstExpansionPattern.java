package vadl.lcb.passes.llvmLowering.tablegen.model;

import java.util.List;
import vadl.viam.graph.Graph;

/**
 * This is tablegen pattern defines a pseudo instruction expansion.
 */
public class TableGenPseudoInstExpansionPattern extends TableGenPattern {

  private final String name;
  private final boolean isCall;
  private final boolean isBranch;
  private final boolean isIndirectBranch;
  private final List<TableGenInstructionOperand> outputs;
  private final List<TableGenInstructionOperand> inputs;
  private final Graph machine;

  /**
   * Constructor.
   */
  public TableGenPseudoInstExpansionPattern(String name,
                                            Graph selector,
                                            Graph machine,
                                            boolean isCall,
                                            boolean isBranch,
                                            boolean isIndirectBranch,
                                            List<TableGenInstructionOperand> inputs,
                                            List<TableGenInstructionOperand> outputs) {
    super(selector);
    this.name = name;
    this.isCall = isCall;
    this.isBranch = isBranch;
    this.isIndirectBranch = isIndirectBranch;
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
        isBranch,
        isIndirectBranch,
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

  public boolean isBranch() {
    return isBranch;
  }

  public boolean isIndirectBranch() {
    return isIndirectBranch;
  }

  public List<TableGenInstructionOperand> outputs() {
    return outputs;
  }

  public List<TableGenInstructionOperand> inputs() {
    return inputs;
  }
}
