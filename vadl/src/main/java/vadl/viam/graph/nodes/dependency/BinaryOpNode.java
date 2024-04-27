package vadl.viam.graph.nodes.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;

public class BinaryOpNode extends BinaryNode {

  @DataValue
  private final String op; // TODO: refactor to real datastructure

  public BinaryOpNode(ExpressionNode x, ExpressionNode y, String op) {
    super(x, y);
    this.op = op;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(op);
  }
}
