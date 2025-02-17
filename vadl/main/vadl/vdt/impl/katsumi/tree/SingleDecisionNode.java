package vadl.vdt.impl.katsumi.tree;

import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.BitVector;

/**
 * Select the child based on a single matching bit pattern
 */
public class SingleDecisionNode implements InnerNode {

  private final BitPattern pattern;
  private final Node matchingChild;
  private final Node otherChild;

  /**
   * Creates a new inner node.
   *
   * @param pattern       the pattern to check
   * @param matchingChild the child to select upon matching the pattern
   * @param otherChild    the child to select if the pattern does not match
   */
  public SingleDecisionNode(BitPattern pattern, Node matchingChild, Node otherChild) {
    this.pattern = pattern;
    this.matchingChild = matchingChild;
    this.otherChild = otherChild;
  }

  @Override
  public Node decide(BitVector insn) {
    return pattern.test(insn) ? matchingChild : otherChild;
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
