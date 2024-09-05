package vadl.viam.graph.control;

import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.SideEffectNode;

public class ProcedureEndNode extends AbstractEndNode {

  public ProcedureEndNode(
      NodeList<SideEffectNode> sideEffects) {
    super(sideEffects);
  }

  @Override
  public Node copy() {
    return new ProcedureEndNode(sideEffects().copy());
  }

  @Override
  public Node shallowCopy() {
    return new ProcedureEndNode(sideEffects());
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }
}
