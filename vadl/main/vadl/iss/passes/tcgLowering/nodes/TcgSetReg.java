package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import java.util.function.Function;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.Register;
import vadl.viam.graph.Node;

/**
 * Translates to a TCG mov operation. The TCGv is written to the
 * given register.
 */
// TODO: Remove this and replace by tcg_gen_move (TcgMoveNode) instead
public class TcgSetReg extends TcgNode {

  @DataValue
  TcgV dest;
  @DataValue
  Register register;

  public TcgSetReg(Register reg, TcgV dest) {
    register = reg;
    this.dest = dest;
  }

  public Register register() {
    return register;
  }

  public TcgV dest() {
    return dest;
  }

  @Override
  public String cCode(Function<Node, String> nodeToCCode) {
    return "";
  }

  @Override
  public Node copy() {
    return new TcgSetReg(register, dest);
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(dest);
    collection.add(register);
  }
}
