package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.types.Type;
import vadl.viam.graph.GraphEdgeVisitor;
import vadl.viam.graph.Node;


/**
 * Represents a unary node in the VIAM graph.
 * A unary node is an expression node with only one input node.
 */
// TODO: Check if we should remove this
public abstract class UnaryNode extends ExpressionNode {

  @Input
  protected ExpressionNode value;

  public UnaryNode(ExpressionNode value, Type type) {
    super(type);
    this.value = value;
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
}
