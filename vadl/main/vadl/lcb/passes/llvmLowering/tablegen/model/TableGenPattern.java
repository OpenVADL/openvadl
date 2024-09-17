package vadl.lcb.passes.llvmLowering.tablegen.model;

import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.model.MachineInstructionNode;
import vadl.viam.graph.Graph;

/**
 * TableGen pattern has a tree for LLVM Dag nodes to select a pattern in the instruction
 * selection. This is represented by {@code selector}.
 * And a tree for the emitted machine instruction. This is represented by {@code machine}.
 */
public record TableGenPattern(Graph selector, Graph machine) {
  /**
   * Copy the {@link #selector} and {@link #machine} and create new object.
   */
  public TableGenPattern copy() {
    return new TableGenPattern(selector.copy(), machine.copy());
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
}
