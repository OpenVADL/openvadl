package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.Canonicalizable;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;


/**
 * Represents a type cast in the VIAM graph.
 * A type cast node is a unary node that casts the value of its input node to a specified type.
 */
public class TypeCastNode extends UnaryNode implements Canonicalizable {

  @DataValue
  private final Type castType;

  public TypeCastNode(ExpressionNode value, Type type) {
    super(value, type);
    this.castType = type;
  }

  @Override
  public void verifyState() {
    super.verifyState();
    ensure(castType instanceof DataType, "Currently casts are only possible to DataTypes");
    ensure(value.type() instanceof DataType, "Type to cast must be DataType");
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(castType);
  }

  /**
   * Get the cast type.
   */
  public Type castType() {
    return this.castType;
  }

  @Override
  public Node copy() {
    return new TypeCastNode((ExpressionNode) value.copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new TypeCastNode(value, type());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public Node canonical() {
    if (value.isConstant()) {
      var constant = ((ConstantNode) value).constant();
      ensure(constant instanceof Constant.Value, "Only value constants may be cast");
      return new ConstantNode(((Constant.Value) constant).castTo((DataType) castType));
    }
    return this;
  }
}
