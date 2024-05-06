package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DummyType;
import vadl.types.Type;

/**
 * An infix operation with two operands.
 */
// TODO: This should probably be removed.
public class BinaryOpNode extends BinaryNode {

  @DataValue
  private final String op; // TODO: refactor to real datastructure

  public BinaryOpNode(ExpressionNode x, ExpressionNode y, String op, Type type) {
    super(x, y, type);
    this.op = op;
  }

  @Override
  public Type type() {
    return DummyType.INSTANCE;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(op);
  }
}
