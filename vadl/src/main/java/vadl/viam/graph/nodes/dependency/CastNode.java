package vadl.viam.graph.nodes.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;

public class CastNode extends UnaryNode {

  @DataValue
  private final String type; // TODO: Refactor by correct datastructure

  public CastNode(ExpressionNode value, String type) {
    super(value);
    this.type = type;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(type);
  }
}
