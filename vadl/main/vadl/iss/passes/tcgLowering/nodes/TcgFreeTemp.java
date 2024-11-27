package vadl.iss.passes.tcgLowering.nodes;

import java.util.function.Function;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.viam.graph.Node;

/**
 * Represents the {@code tcg_temp_free} TCG function.
 */
public class TcgFreeTemp extends TcgVarNode {

  public TcgFreeTemp(TcgV variable) {
    super(variable);
  }

  @Override
  public void verifyState() {
    super.verifyState();
    ensure(variable().kind() == TcgV.Kind.TMP, "Can only free temporary variables");
  }

  @Override
  public Node copy() {
    return new TcgFreeTemp(variable());
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "tcg_temp_free_i" + variable().width().width + "(" + variable().varName() + ");";
  }
}
