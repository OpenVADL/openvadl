package vadl.viam.graph.nodes.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;

/**
 * The constant node represents a compile time constant value in the
 * data dependency graph.
 */
public class ConstantNode extends ExpressionNode {

  @DataValue
  int constant; // TODO: Replace this by appropriate type


  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(constant);
  }
}
