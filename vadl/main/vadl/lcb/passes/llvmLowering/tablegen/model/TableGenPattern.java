package vadl.lcb.passes.llvmLowering.tablegen.model;

import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.domain.machineDag.MachineInstructionNode;
import vadl.viam.graph.Graph;

/**
 * TableGen pattern has a tree for LLVM Dag nodes to select a pattern in the instruction
 * selection. This is represented by {@code selector}.
 * And a tree for the emitted machine instruction. This is represented by {@code machine}.
 */
public abstract class TableGenPattern {
  protected final Graph selector;

  protected TableGenPattern(Graph selector) {
    this.selector = selector;
  }

  /**
   * Checks whether the {@code selector} is a valid TableGen pattern.
   *
   * @return true if ok.
   */
  public boolean isPatternLowerable() {
    return selector.getDataflowRoots().stream().allMatch(node ->
        node instanceof LlvmNodeLowerable
            || node instanceof MachineInstructionNode);
  }

  public Graph selector() {
    return selector;
  }
}
