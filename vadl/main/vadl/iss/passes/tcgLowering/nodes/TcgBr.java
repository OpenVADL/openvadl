package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.function.Function;
import vadl.iss.passes.tcgLowering.TcgLabel;
import vadl.viam.graph.Node;

/**
 * Represents a TCG (Tiny Code Generation) branch operation node.
 * Extends the TcgOpNode class by including a TcgLabel
 * to denote the target label for the branch operation.
 */
public class TcgBr extends TcgLabelNode {

  /**
   * Constructs a TcgBr (Tiny Code Generation Branch) object with the specified label.
   *
   * @param label The target label for the branch operation. This label
   *              represents the destination to which the branch will jump.
   */
  public TcgBr(TcgLabel label) {
    super(label);
  }


  @Override
  public Node copy() {
    return new TcgBr(label());
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "tcg_gen_br(" + label().varName() + ");";
  }
}
