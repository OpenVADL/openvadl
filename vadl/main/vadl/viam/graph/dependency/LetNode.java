package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.javaannotations.viam.Input;
import vadl.viam.Identifier;
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
  protected Identifier identifier;

  @Input
  protected ExpressionNode expression;

  /**
   * Constructs a let-node.
   *
   * @param identifier the name of the let assignment
   * @param expression the value of the let assignment
   */
  public LetNode(Identifier identifier, ExpressionNode expression) {
    super(expression.type());
    this.identifier = identifier;
    this.expression = expression;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(identifier);
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
}
