package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;

public class TypeCastNode extends UnaryNode {

  @DataValue
  private final String type; // TODO: Refactor by correct datastructure

  public TypeCastNode(ExpressionNode value, String type) {
    super(value);
    this.type = type;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(type);
  }
}
