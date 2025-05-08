package vadl.vdt.impl.irregular.tree;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.utils.Bit;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.BitVector;

/**
 * Simple implementation of an inner node, deciding the matching child node based on pattern
 * matching.
 */
public class MultiDecisionNode extends AbstractTruncatingDecisionNode {

  private final Map<BitPattern, Node> children;

  /**
   * Creates a new inner node.
   *
   * @param offset   The offset of bits to skip prior to matching
   * @param length   The number of bits to match
   * @param children The children to match against
   */
  public MultiDecisionNode(int offset, int length, Map<BitPattern, Node> children) {
    super(offset, length);
    this.children = children;
  }

  @Override
  public Node decide(BitVector insn) {

    // extend/truncate the instruction to the relevant bits before testing
    final BitVector i = insn
        .rightPad(getOffset() + getLength(), new Bit(false))
        .truncate(getOffset(), getLength());

    for (Map.Entry<BitPattern, Node> entry : children.entrySet()) {
      if (entry.getKey().test(i)) {
        return entry.getValue();
      }
    }

    throw new RuntimeException("No decision found for " + insn);
  }

  public Map<BitPattern, Node> getChildren() {
    return children;
  }

  @Override
  public Collection<Node> children() {
    return new HashSet<>(children.values());
  }

  @Override
  public <T> @Nullable T accept(Visitor<T> visitor) {
    return visitor.visit(this);
  }
}
