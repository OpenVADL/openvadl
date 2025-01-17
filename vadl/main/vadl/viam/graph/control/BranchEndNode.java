package vadl.viam.graph.control;

import javax.annotation.Nonnull;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * Represents the end of a control subflow (e.g. if branch).
 */
public class BranchEndNode extends AbstractEndNode {
  public BranchEndNode(
      NodeList<SideEffectNode> sideEffects) {
    super(sideEffects);
  }

  @Override
  @Nonnull
  public DirectionalNode predecessor() {
    var superNode = super.predecessor();
    ensure(superNode instanceof DirectionalNode, "Invalid predecessor %s", superNode);
    return (DirectionalNode) superNode;
  }

  @Override
  public Node copy() {
    return new BranchEndNode(
        new NodeList<>(sideEffects().stream().map(x -> (SideEffectNode) x.copy()).toList()));
  }

  @Override
  public Node shallowCopy() {
    return new BranchEndNode(sideEffects());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
