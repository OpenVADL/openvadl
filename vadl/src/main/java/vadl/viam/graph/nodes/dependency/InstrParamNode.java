package vadl.viam.graph.nodes.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;

public class InstrParamNode extends ParamNode {

  @DataValue
  String reg; // TODO: refactor to real datastructure

  public InstrParamNode(String reg) {
    this.reg = reg;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(reg);
  }
}
