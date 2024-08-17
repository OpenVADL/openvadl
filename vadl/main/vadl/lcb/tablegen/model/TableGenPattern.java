package vadl.lcb.tablegen.model;

import vadl.viam.graph.Graph;

/**
 * TableGen pattern has a tree for LLVM Dag nodes to select a pattern in the instruction
 * selection. This is represented by {@code selector}.
 * And a tree for the emitted machine instruction. This is represented by {@code machine}.
 */
public class TableGenPattern {
  private final Graph selector;
  private final Graph machine;

  public TableGenPattern(Graph selector, Graph machine) {
    this.selector = selector;
    this.machine = machine;
  }

  public Graph selector() {
    return selector;
  }

  public Graph machine() {
    return machine;
  }
}
