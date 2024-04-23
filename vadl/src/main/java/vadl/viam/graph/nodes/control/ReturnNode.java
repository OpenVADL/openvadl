package vadl.viam.graph.nodes.control;

import vadl.viam.graph.NodeList;
import vadl.viam.graph.nodes.dependency.SideEffectNode;


/**
 * Represents the end node of the control flow graph of a pure function.
 */
public class ReturnNode extends AbstractEndNode {
  public ReturnNode(NodeList<SideEffectNode> sideEffects) {
    super(sideEffects);
  }
}
