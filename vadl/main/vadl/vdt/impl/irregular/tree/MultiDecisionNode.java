package vadl.vdt.impl.irregular.tree;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.BitVector;

/**
 * Simple implementation of an inner node, deciding the matching child node based on pattern
 * matching.
 */
public class MultiDecisionNode implements InnerNode {

  private final Map<BitPattern, Node> children;

  /**
   * Creates a new inner node.
   *
   * @param children the children to match against
   */
  public MultiDecisionNode(Map<BitPattern, Node> children) {
    this.children = children;
  }

  @Override
  public Node decide(BitVector insn) {

    for (Map.Entry<BitPattern, Node> entry : children.entrySet()) {
      if (entry.getKey().test(insn)) {
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
