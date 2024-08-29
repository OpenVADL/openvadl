package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
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
public class LetNode extends ExpressionNode {
  // TODO: Add label functionality

  @DataValue
  protected Name name;

  @Input
  protected ExpressionNode expression;

  /**
   * Constructs a let-node.
   *
   * @param name       the name of the let assignment
   * @param expression the value of the let assignment
   */
  public LetNode(Name name, ExpressionNode expression) {
    super(expression.type());
    this.name = name;
    this.expression = expression;
  }

  public Name letName() {
    return name;
  }

  public ExpressionNode expression() {
    return expression;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(name);
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
  public Node copy() {
    return new LetNode(name, (ExpressionNode) expression.copy());
  }

  @Override
  public Node shallowCopy() {
    return new LetNode(name, expression);
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
