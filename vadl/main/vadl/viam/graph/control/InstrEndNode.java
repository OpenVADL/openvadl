package vadl.viam.graph.control;


import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * The InstrEndNode class represents the end node of a control flow graph of some Instruction.
 */
public class InstrEndNode extends AbstractEndNode {
  public InstrEndNode(
      NodeList<SideEffectNode> sideEffects) {
    super(sideEffects);
  }

  @Override
  public Node copy() {
    return new InstrEndNode(
        new NodeList<>(sideEffects().stream().map(x -> (SideEffectNode) x.copy()).toList()));
  }

  @Override
  public Node shallowCopy() {
    return new InstrEndNode(sideEffects());
  }

  @Override
  public void accept(GraphNodeVisitor visitor) {
    visitor.visit(this);
  }
}
