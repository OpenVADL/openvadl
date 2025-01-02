package vadl.vdt.impl.theiling;

import javax.annotation.Nullable;
import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Visitor;
import vadl.vdt.utils.Instruction;

/**
 * Simple implementation of a leaf node, holding an instruction.
 *
 * @param instruction the instruction to match
 */
public record LeafNodeImpl(Instruction instruction) implements LeafNode {

  @Override
  public <T> @Nullable T accept(Visitor<T> visitor) {
    return visitor.visit(this);
  }
}
