package vadl.iss.passes.tcgLowering.nodes;

import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.viam.graph.Node;

/**
 * Represents the {@code tcg_gen_not} TCG instruction in the TCG VIAM lowering.
 */
public class TcgNotNode extends TcgBinaryOpNode {

  public TcgNotNode(TcgVRefNode resVar, TcgVRefNode arg1, TcgVRefNode arg2) {
    super(resVar, arg1, arg2, resVar.width());
  }

  @Override
  public String tcgFunctionName() {
    return "tcg_gen_not";
  }

  @Override
  public Node copy() {
    return new TcgNotNode(firstDest().copy(TcgVRefNode.class), arg1.copy(TcgVRefNode.class),
        arg2.copy(TcgVRefNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new TcgNotNode(firstDest(), arg1, arg2);
  }
}
