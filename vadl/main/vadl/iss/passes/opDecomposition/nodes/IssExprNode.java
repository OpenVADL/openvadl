package vadl.iss.passes.opDecomposition.nodes;

import vadl.types.Type;
import vadl.viam.graph.dependency.ExpressionNode;

public abstract class IssExprNode extends ExpressionNode {
  public IssExprNode(Type type) {
    super(type);
  }
}
