package vadl.iss.passes.tcgLowering.nodes;

import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.viam.graph.Node;

/**
 * A node that doesn’t emit any operation, however is necessary as replacement of some
 * op node that is not being emitted. Without this, the register allocation would have
 * a gap that can't be filled.
 */
public class TcgBiNopNode extends TcgBinaryOpNode {

  public TcgBiNopNode(TcgVRefNode dest, TcgVRefNode arg1, TcgVRefNode arg2) {
    super(dest, arg1, arg2);
  }

  @Override
  public String tcgFunctionName() {
    return "";
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "";
  }

  @Override
  public Node copy() {
    return new TcgBiNopNode(dest.copy(), arg1.copy(), arg2.copy());
  }

  @Override
  public Node shallowCopy() {
    return new TcgBiNopNode(dest, arg1, arg2);
  }
}
