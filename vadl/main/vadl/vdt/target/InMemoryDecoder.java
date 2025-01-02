package vadl.vdt.target;

import vadl.vdt.model.InnerNode;
import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.utils.BitVector;
import vadl.viam.Instruction;

/**
 * A decoder that uses an in-memory decision tree to decode instructions. Useful for testing and
 * debugging.
 */
public class InMemoryDecoder implements Visitor<Instruction> {

  private final Node decisionTree;
  private final ThreadLocal<BitVector> encoding = new ThreadLocal<>();

  public InMemoryDecoder(Node tree) {
    this.decisionTree = tree;
  }

  public Instruction decide(BitVector insn) {
    this.encoding.set(insn);
    final Instruction decision = decisionTree.accept(this);
    this.encoding.remove();
    return decision;
  }

  @Override
  public Instruction visit(InnerNode node) {
    final BitVector encoding = this.encoding.get();
    final Node decide = node.decide(encoding);
    if (decide == null) {
      throw new RuntimeException("No decision found for " + encoding);
    }
    return decide.accept(this);
  }

  @Override
  public Instruction visit(LeafNode node) {
    return node.instruction();
  }
}
