package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.Register;
import vadl.viam.graph.Node;

/**
 * Translates to a TCG mov operation. The TCGv is written to the
 * given register.
 */
// TODO: Rename this to TcgMov and do not directly use register but dest var
public class TcgSetReg extends TcgOpNode {

  @DataValue
  Register register;

  public TcgSetReg(Register reg, TcgV res) {
    super(res, res.width());
    register = reg;
  }

  public Register register() {
    return register;
  }

  @Override
  public Node copy() {
    return new TcgSetReg(register, res);
  }

  @Override
  public Node shallowCopy() {
    return copy();
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(register);
  }
}
