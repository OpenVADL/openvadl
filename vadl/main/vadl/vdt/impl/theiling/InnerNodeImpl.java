package vadl.vdt.impl.theiling;

import java.util.Map;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.BitVector;

public class InnerNodeImpl implements InnerNode {

  private final Map<BitPattern, Node> children;

  public InnerNodeImpl(Map<BitPattern, Node> children) {
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

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visit(this);
  }
}
