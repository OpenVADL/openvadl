package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.types.TupleType;
import vadl.types.Type;
import vadl.utils.SourceLocation;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Represents a let expression in the VADL Specification.
 *
 * <p>It stores the identifier as well as the label (once implemented)
 * to allow generating code with meaningful variable names.
 */
public class TupleGetFieldNode extends ExpressionNode {

  @DataValue
  private int index;

  @Input
  private ExpressionNode expression;

  /**
   * Constructs TupleGetFieldNode.
   *
   * @param index      the index to get
   * @param expression the value that returns a tuple
   */
  public TupleGetFieldNode(int index, ExpressionNode expression, Type type) {
    super(type);
    this.expression = expression;
    this.index = index;
  }

  @Override
  public void verifyState() {
    super.verifyState();
    ensure(index >= 0, "Index is negative.");
    ensure(expression.type() instanceof TupleType, "The expression result not in tuple, but in %s",
        expression.type());
    ensure(index < ((TupleType) expression.type()).size(),
        "The index of is out of bound. i: %s, tuple: %s", index, expression.type());
    ensure(((TupleType) expression.type()).get(index).isTrivialCastTo(type()),
        "The node's type does not match the type retrieved from the expression.");
  }

  public int index() {
    return index;
  }

  public ExpressionNode expression() {
    return expression;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(index);
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(expression);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    expression = visitor.apply(this, expression, ExpressionNode.class);
  }

  @Override
  public ExpressionNode copy() {
    return new TupleGetFieldNode(index, (ExpressionNode) expression.copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new TupleGetFieldNode(index, expression, type());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }


  /**
   * The name of a let expression with source location.
   */
  public record Name(
      String name,
      SourceLocation location
  ) {

    @Override
    public String toString() {
      return name;
    }
  }
}
