package vadl.iss.passes.tcgLowering.nodes;

import vadl.iss.passes.tcgLowering.TcgV;
import vadl.iss.passes.tcgLowering.TcgWidth;
import vadl.javaannotations.viam.Input;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * TcgConstantNode represents a node containing a constant value in the TCG.
 * This node extends TcgUnaryImmOpNode and encapsulates the result variable and
 * the constant value as an ExpressionNode.
 */
public class TcgConstantNode extends TcgUnaryImmOpNode {

  public TcgConstantNode(TcgV res, ExpressionNode value) {
    super(res, value);
  }

  @Override
  public Node copy() {
    return new TcgConstantNode(res, arg.copy(ExpressionNode.class));
  }

  @Override
  public Node shallowCopy() {
    return new TcgConstantNode(res, arg);
  }


  @Override
  public String tcgFunctionName() {
    return "tcg_constant_i" + res.width().width;
  }
}
