package vadl.vdt.target;

import java.util.Objects;
import javax.annotation.Nullable;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.utils.BitVector;
import vadl.vdt.utils.Instruction;

/**
 * A decoder that uses an in-memory decision tree to decode instructions. Useful for testing and
 * debugging.
 */
public class InMemoryDecoder implements Visitor<Instruction> {

  private final Node decisionTree;
  private @Nullable BitVector encoding;

  public InMemoryDecoder(Node tree) {
    this.decisionTree = tree;
  }

  public Instruction decide(BitVector insn) {
    this.encoding = insn;
    return Objects.requireNonNull(decisionTree.accept(this));
  }

  @Override
  public Instruction visit(InnerNode node) {
    final BitVector encoding = Objects.requireNonNull(this.encoding);
    final Node decide = node.decide(encoding);
    if (decide == null) {
      throw new IllegalArgumentException("No decision found for " + encoding);
    }
    return Objects.requireNonNull(decide.accept(this));
  }

  @Override
  public Instruction visit(LeafNode node) {
    return node.instruction();
  }
}
