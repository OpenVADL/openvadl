package vadl.vdt.impl.theiling;

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
 * Simple implementation of an inner node, holding a mask, a fallback node and a map of children.
 * A decision is based on the bits specified by the mask and depending on the matching child bit
 * pattern. If no child matches, the fallback node is returned (if present).
 */
public class InnerNodeImpl implements InnerNode {

  private final BitVector mask;
  private final @Nullable Node fallback;
  private final Map<BitPattern, Node> children;

  /**
   * Creates a new inner node.
   *
   * @param mask     the mask specifying the bits to consider
   * @param children the children to match against
   * @param fallback the fallback node to return if no child matches
   */
  public InnerNodeImpl(BitVector mask, Map<BitPattern, Node> children, @Nullable Node fallback) {
    this.mask = mask;
    this.children = children;
    this.fallback = fallback;
  }

  @Override
  public Node decide(BitVector insn) {

    for (Map.Entry<BitPattern, Node> entry : children.entrySet()) {
      if (entry.getKey().test(insn)) {
        return entry.getValue();
      }
    }

    if (fallback != null) {
      return fallback;
    }

    throw new RuntimeException("No decision found for " + insn);
  }

  public BitVector getMask() {
    return mask;
  }

  public @Nullable Node getFallback() {
    return fallback;
  }

  public Map<BitPattern, Node> getChildren() {
    return children;
  }

  @Override
  public Collection<Node> children() {
    var result = new HashSet<>(children.values());
    if (fallback != null) {
      result.add(fallback);
    }
    return result;
  }

  @Override
  public <T> @Nullable T accept(Visitor<T> visitor) {
    return visitor.visit(this);
  }
}
