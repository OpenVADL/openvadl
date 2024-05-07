package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DummyType;
import vadl.types.Type;
import vadl.viam.Constant;

/**
 * The constant node represents a compile time constant value in the
 * data dependency graph.
 */
public class ConstantNode extends ExpressionNode {

  @DataValue
  public final Constant constant; // TODO: Replace this by appropriate type

  public ConstantNode(Constant constant, Type type) {
    super(type);
    this.constant = constant;
  }

//  @Override
//  public Type type() {
//    return constant.type();
//  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(constant);
  }
}
