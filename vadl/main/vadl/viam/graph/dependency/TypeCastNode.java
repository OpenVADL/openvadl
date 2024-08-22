package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.Canonicalizable;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;


/**
 * Represents a type cast in the VIAM graph.
 * A type cast node is a unary node that casts the value of its input node to a specified type.
 *
 * <p><b>NOTE: </b> Never create a type cast node during a pass. You also don't have to handle
 * type cast nodes, as they are eliminated right after frontend
 * (see {@link vadl.viam.passes.typeCastElimination.TypeCastEliminationPass}. In future we will
 * probably completely remove type cast nodes from the VIAM. </p>
 *
 * @see SignExtendNode
 * @see ZeroExtendNode
 * @see TruncateNode
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


  /**
   * Get the cast type.
   */
  public Type castType() {
    return this.castType;
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

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(castType);
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
    // It is an error as no type casts should be contained in any behavior
    // (they get eliminated right at the beginning)
    throw new ViamGraphError(
        "Accept on the TypeCastNode is not allowed and indicates a logic error.")
        .addContext(this)
        .addContext(this.graph());
  }

}
