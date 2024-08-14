package vadl.lcb.passes.llvmLowering;

/**
 * This interface indicates how a SelectionDagNode in LLVM should be lowered in TableGen.
 */
public interface LlvmNodeLowerable {
  /**
   * Lowers the node into a TableGen definition.
   *
   * @return a string for TableGen.
   */
   String lower();
}
