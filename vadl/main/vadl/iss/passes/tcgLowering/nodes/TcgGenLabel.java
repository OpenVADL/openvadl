package vadl.iss.passes.tcgLowering.nodes;

import java.util.function.Function;
import vadl.iss.passes.tcgLowering.TcgLabel;
import vadl.viam.graph.Node;

/**
 * Represents a TCG (Tiny Code Generation) label generation node.
 */
public class TcgGenLabel extends TcgLabelNode {

  /**
   * Creates a TcgGenLabel instance which represents a TCG label generation node.
   *
   * @param label The TCG label associated with this node.
   */
  public TcgGenLabel(TcgLabel label) {
    super(label);
  }


  @Override
  public Node copy() {
    return new TcgGenLabel(label());
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "TCGLabel *" + label().varName() + " = gen_new_label();";
  }
}
