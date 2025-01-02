package vadl.vdt.impl.theiling;

import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Visitor;
import vadl.viam.Instruction;

public record LeafNodeImpl(Instruction instruction) implements LeafNode {

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visit(this);
  }
}
