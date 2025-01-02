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

public class InnerNodeImpl implements InnerNode {

  private final @Nullable Node fallback;
  private final Map<BitPattern, Node> children;

  public InnerNodeImpl(Map<BitPattern, Node> children, @Nullable Node fallback) {
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
