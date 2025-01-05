package vadl.vdt.model;

import vadl.vdt.utils.Instruction;

/**
 * Leaf node of the decode decision tree, representing a single instruction.
 */
public interface LeafNode extends Node {

  Instruction instruction();

}
