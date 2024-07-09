package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.Type;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.InstrCallNode;


/**
 * Represents a type cast in the VIAM graph.
 * A type cast node is a unary node that casts the value of its input node to a specified type.
 */
public class TypeCastNode extends UnaryNode {

  @DataValue
  private final Type castType;

  public TypeCastNode(ExpressionNode value, Type type) {
    super(value, type);
    this.castType = type;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(castType);
  }

  @Override
  public Node copy() {
    return new TypeCastNode((ExpressionNode) value.copy(), type());
  }
}
