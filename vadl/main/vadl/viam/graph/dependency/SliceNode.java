package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.GraphEdgeVisitor;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;

/**
 * A node that represents the bit slice operation on a value.
 */
public class SliceNode extends ExpressionNode {

  @DataValue
  protected Constant.BitSlice slice;

  @Input
  protected ExpressionNode value;

  /**
   * Constructs a new SliceNode.
   *
   * @param value The value from which the bit slice is taken.
   * @param slice The bit slice represented by this node.
   * @param type  The result type of the node.
   */
  public SliceNode(ExpressionNode value, Constant.BitSlice slice, DataType type) {
    super(type);

    this.value = value;
    this.slice = slice;

    verifyState();
  }

  public Constant.BitSlice bitSlice() {
    return slice;
  }

  public ExpressionNode value() {
    return value;
  }

  @Override
  public DataType type() {
    return (DataType) super.type();
  }

  @Override
  public void verifyState() {
    super.verifyState();

    ensure(Type.bits(slice.bitSize()).canBeCastTo(type()),
        "Slice type cannot be cast to node type: %s vs %s",
        slice.type(), type());
    ensure(value.type() instanceof DataType, "Value node must have a data type.");
    ensure(((DataType) value.type()).bitWidth() > slice.msb(),
        "Value node must have at least %d bits to be sliceable by %s",
        slice.msb() + 1, slice);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(slice);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(value);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphEdgeVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    value = visitor.apply(this, value, ExpressionNode.class);
  }

  @Override
  public Node copy() {
    return new SliceNode((ExpressionNode) value.copy(), slice, type());
  }

  @Override
  public Node shallowCopy() {
    return new SliceNode(value, slice, type());
  }

  @Override
  public String generateOopExpression() {
    throw new RuntimeException("not implemented");
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
