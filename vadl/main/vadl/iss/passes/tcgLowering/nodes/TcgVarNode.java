package vadl.iss.passes.tcgLowering.nodes;

import java.util.List;
import vadl.iss.passes.tcgLowering.TcgV;
import vadl.javaannotations.viam.DataValue;

abstract public class TcgVarNode extends TcgNode {

  @DataValue
  private TcgV variable;

  public TcgVarNode(TcgV variable) {
    this.variable = variable;
  }

  public TcgV variable() {
    return variable;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(variable);
  }
}
