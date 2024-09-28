package vadl.lcb.passes.llvmLowering.tablegen.model;

import vadl.viam.graph.Graph;

/**
 * This is tablegen pattern which has a selector and emits a machine instruction.
 */
public class TableGenSelectionMachinePattern extends TableGenPattern {

  private final Graph machine;

  public TableGenSelectionMachinePattern(Graph selector, Graph machine) {
    super(selector);
    this.machine = machine;
  }

  /**
   * Copy the {@code selector} and {@link #machine} and create new object.
   */
  public TableGenPattern copy() {
    return new TableGenSelectionMachinePattern(selector.copy(), machine.copy());
  }

  public Graph machine() {
    return machine;
  }
}
