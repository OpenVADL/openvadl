package vadl.viam.graph.control;

import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * Represents the end of a control subflow (e.g. if branch).
 */
public class EndNode extends AbstractEndNode {
  public EndNode(
      NodeList<SideEffectNode> sideEffects) {
    super(sideEffects);
  }
}
