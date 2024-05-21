package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.Type;
import vadl.viam.Parameter;

public class FuncParamNode extends ParamNode {

  @DataValue
  private final Parameter parameter;

  public FuncParamNode(Parameter parameter, Type type) {
    super(type);
    ensure(type.equals(parameter.type()), "Parameter type mismatch");
    this.parameter = parameter;
  }

  public Parameter parameter() {
    return parameter;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(parameter);
  }
}
