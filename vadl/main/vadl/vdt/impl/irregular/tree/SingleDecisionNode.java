package vadl.vdt.impl.irregular.tree;

import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.utils.Bit;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.BitVector;

/**
 * Select the child based on a single matching bit pattern
 */
public class SingleDecisionNode extends AbstractTruncatingDecisionNode {

  private final BitPattern pattern;
  private final Node matchingChild;
  private final Node otherChild;

  /**
   * Creates a new inner node.
   *
   * @param offset        the offset of bits to skip prior to matching
   * @param length        the number of bits to match
   * @param pattern       the pattern to check
   * @param matchingChild the child to select upon matching the pattern
   * @param otherChild    the child to select if the pattern does not match
   */
  public SingleDecisionNode(int offset, int length, BitPattern pattern, Node matchingChild,
                            Node otherChild) {
    super(offset, length);
    this.pattern = pattern;
    this.matchingChild = matchingChild;
    this.otherChild = otherChild;
  }

  @Override
  public Node decide(BitVector insn) {

    // extend/truncate the instruction to the relevant bits before testing
    final BitVector i = insn
        .rightPad(getOffset() + getLength(), new Bit(false))
        .truncate(getOffset(), getLength());

    return pattern.test(i) ? matchingChild : otherChild;
  }

  public BitPattern getPattern() {
    return pattern;
  }

  public Node getMatchingChild() {
    return matchingChild;
  }

  public Node getOtherChild() {
    return otherChild;
  }

  @Override
  public Collection<Node> children() {
    return Set.of(matchingChild, otherChild);
  }

  @Override
  public <T> @Nullable T accept(Visitor<T> visitor) {
    return visitor.visit(this);
  }
}
