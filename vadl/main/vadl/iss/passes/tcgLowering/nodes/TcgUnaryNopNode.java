package vadl.iss.passes.tcgLowering.nodes;

import java.util.function.Function;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.viam.graph.Node;

/**
 * A node that doesn’t emit any operation, however is necessary as replacement of some
 * op node that is not being emitted. Without this, the register allocation would have
 * a gap that can't be filled.
 */
public class TcgUnaryNopNode extends TcgUnaryOpNode {

  public TcgUnaryNopNode(TcgVRefNode dest, TcgVRefNode arg) {
    super(dest, arg);
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
    return new TcgUnaryNopNode(firstDest().copy(), arg);
  }

  @Override
  public Node shallowCopy() {
    return new TcgUnaryNopNode(firstDest(), arg);
  }
}
