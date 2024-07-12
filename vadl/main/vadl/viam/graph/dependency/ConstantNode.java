package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.viam.Constant;
import vadl.viam.graph.Node;

/**
 * The constant node represents a compile time constant value in the
 * data dependency graph.
 */
public class ConstantNode extends ExpressionNode {

  @DataValue
  public Constant constant;

  public ConstantNode(Constant constant) {
    super(constant.type());
    this.constant = constant;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(constant);
  }

  @Override
  public Node copy() {
    return new ConstantNode(constant);
  }

  @Override
  public Node shallowCopy() {
    return new ConstantNode(constant);
  }
}
