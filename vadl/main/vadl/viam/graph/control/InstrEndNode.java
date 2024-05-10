package vadl.viam.graph.control;


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
}
