package vadl.viam.graph.control;

import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.SideEffectNode;


/**
 * Represents the end node of the control flow graph of a pure function.
 */
public class ReturnNode extends AbstractEndNode {

  public ExpressionNode value;

  public ReturnNode(ExpressionNode value, NodeList<SideEffectNode> sideEffects) {
    super(sideEffects);
    this.value = value;
  }
}
