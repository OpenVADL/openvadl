package vadl.vdt.model;

import java.util.Collection;
import vadl.vdt.utils.BitVector;

/**
 * Inner node of the decode decision tree, representing a decision to be made.
 */
public interface InnerNode extends Node {

  /**
   * Decide the next node, depending on the concrete instruction encoding provided by {@code insn}.
   *
   * @param insn The concrete encoding of an instruction to decode
   * @return The next node in the decision tree
   */
  Node decide(BitVector insn);

  /**
   * The children of this decision node.
   *
   * @return The children of this decision node
   */
  Collection<Node> children();
}
