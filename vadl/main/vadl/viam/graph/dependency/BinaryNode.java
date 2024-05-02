package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * Represents a node with two inputs.
 */
// TODO: Think about removing this class
public abstract class BinaryNode extends ExpressionNode {

  @Input
  protected ExpressionNode left;
  @Input
  protected ExpressionNode right;

  /**
   * Construct a BinaryNode.
   */
  public BinaryNode(ExpressionNode left, ExpressionNode right, Type type) {
    super(type);
    this.left = left;
    this.right = right;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(left);
    collection.add(right);
  }

  @Override
  protected void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    left = visitor.apply(this, left, ExpressionNode.class);
    right = visitor.apply(this, right, ExpressionNode.class);
  }
}
