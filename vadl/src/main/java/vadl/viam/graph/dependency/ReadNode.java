package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;

/**
 * The ReadNode class is an abstract class that extends ExpressionNode
 * and represents a node that reads a value from a location.
 * It provides a common structure and behavior for reading nodes.
 */
public abstract class ReadNode extends ExpressionNode {

  @Input
  ExpressionNode location;

  public ReadNode(ExpressionNode location) {
    this.location = location;
  }

  @Override
  protected void collectInputs(List<Node> collection) {
    super.collectInputs(collection);
    collection.add(location);
  }

  @Override
  public void applyOnInputsUnsafe(GraphVisitor.Applier<Node> visitor) {
    super.applyOnInputsUnsafe(visitor);
    location = visitor.apply(this, location, ExpressionNode.class);
  }
}
