package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DummyType;
import vadl.viam.ConstantValue;

/**
 * The constant node represents a compile time constant value in the
 * data dependency graph.
 */
public class ConstantNode extends ExpressionNode {

  @DataValue
  public final ConstantValue constant; // TODO: Replace this by appropriate type

  public ConstantNode(ConstantValue constant) {
    super(DummyType.INSTANCE);
    this.constant = constant;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(constant);
  }
}
