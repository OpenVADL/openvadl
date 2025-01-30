package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.Set;
import vadl.iss.passes.nodes.TcgVRefNode;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.graph.Node;

/**
 * Represent the {@code tcg_gen_div} and {@code tcg_gen_divu} TCG operations.
 * Depending on {@link #isSigned()} the former or latter is the correct.
 */
public class TcgDivNode extends TcgBinaryOpNode {

  @DataValue
  private boolean signed;

  public TcgDivNode(boolean signed, TcgVRefNode dest, TcgVRefNode arg1,
                    TcgVRefNode arg2) {
    super(dest, arg1, arg2);
    this.signed = signed;
  }

  public boolean isSigned() {
    return signed;
  }

  @Override
  public String tcgFunctionName() {
    var sign = signed ? "" : "u";
    return "tcg_gen_div" + sign;
  }

  @Override
  public Node copy() {
    return new TcgDivNode(signed, firstDest().copy(), arg1().copy(), arg2().copy());
  }

  @Override
  public Node shallowCopy() {
    return new TcgDivNode(signed, firstDest(), arg1(), arg2());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(signed);
  }
}
