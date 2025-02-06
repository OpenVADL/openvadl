package vadl.iss.passes.nodes;

import java.util.List;
import vadl.iss.passes.opDecomposition.nodes.IssExprNode;
import vadl.iss.passes.tcgLowering.TcgExtend;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.types.DataType;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * An ISS-specific expression node that extracts the {@code fromWidth} lowest significant bits
 * from the input value and (zero or sign) extends it to a value of size {@code toWidth}.
 * By setting the toWidth lower than the fromWidth, it also represents truncation of the value.
 * It is used by the ISS to lift/normalize the types to 32 or 64 bit, which is necessary
 * to ensure correct functionality in QEMU.
 */
public class IssExtractNode extends IssExprNode {

  @Input
  private ExpressionNode value;

  @DataValue
  private final int fromWidth;
  @DataValue
  private final int toWidth;
  @DataValue
  private final TcgExtend extendMode;

  /**
   * Constructs the extract node.
   *
   * @param value      which is extracted
   * @param extendMode sign or zero extension mode
   * @param fromWidth  the width of the original value that gets extracted
   * @param toWidth    the target width to which the width is extended/truncated
   * @param type       the original type of expression.
   *                   This is not the target-size even if the
   *                   toWidth parameter was set higher than this type.
   */
  public IssExtractNode(ExpressionNode value, TcgExtend extendMode, int fromWidth,
                        int toWidth,
                        DataType type) {
    super(type);
    this.value = value;
    this.extendMode = extendMode;
    this.toWidth = toWidth;
    this.fromWidth = fromWidth;
  }

  public ExpressionNode value() {
    return value;
  }

  public int fromWidth() {
    return fromWidth;
  }

  public int toWidth() {
    return toWidth;
  }

  public TcgExtend extendMode() {
    return extendMode;
  }

  public boolean isTruncate() {
    return toWidth <= fromWidth;
  }

  public boolean isSigned() {
    return extendMode == TcgExtend.SIGN;
  }

  /**
   * Returns the number of lsb bits that are preserved from the original value.
   * For expansion this is the fromWidth, for truncation it is the toWidth.
   */
  public int preservedWidth() {
    return Math.min(fromWidth, toWidth);
  }

  @Override
  public DataType type() {
    return (DataType) super.type();
  }

  @Override
  public IssExtractNode copy() {
    return new IssExtractNode(value.copy(), extendMode, fromWidth, toWidth, type());
  }

  @Override
  public Node shallowCopy() {
    return new IssExtractNode(value, extendMode, fromWidth, toWidth, type());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {

  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(fromWidth);
    collection.add(toWidth);
    collection.add(extendMode);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(value);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    value = visitor.apply(this, value, ExpressionNode.class);
  }
}
