package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.Type;
import vadl.viam.Parameter;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.InstrCallNode;

/**
 * Represents a parameter node for a function in VADL specification.
 *
 * <p>This node does only exist in graphs that belong to functions.
 */
public class FuncParamNode extends ParamNode {

  @DataValue
  protected Parameter parameter;

  /**
   * Constructs a FuncParamNode instance with a given parameter and type.
   * The node type and parameter type must be equal.
   */
  public FuncParamNode(Parameter parameter) {
    super(parameter.type());
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

  @Override
  public Node copy() {
    return new FuncParamNode(parameter);
  }
}
