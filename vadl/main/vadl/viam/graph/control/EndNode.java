package vadl.viam.graph.control;

import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * Represents the end of a control subflow (e.g. if branch).
 */
public class EndNode extends AbstractEndNode {
  public EndNode(
      NodeList<SideEffectNode> sideEffects) {
    super(sideEffects);
  }

  @Override
  public Node copy() {
    return new EndNode(
        new NodeList<>(sideEffects.stream().map(x -> (SideEffectNode) x.copy()).toList()));
  }

  @Override
  public void canonicalize() {

  }

  @Override
  public Node shallowCopy() {
    return new EndNode(sideEffects);
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
