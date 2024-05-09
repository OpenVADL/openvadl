package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DummyType;
import vadl.types.Type;

/**
 * A format field reference that may be used as parameter to an instruction.
 */
// TODO: Think about an other name.
public class InstrParamNode extends ParamNode {

  @DataValue
  String reg; // TODO: refactor to real datastructure

  public InstrParamNode(String reg, Type type) {
    super(type);
    this.reg = reg;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(reg);
  }
}
