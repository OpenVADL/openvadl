package vadl.iss.passes.opDecomposition.nodes;

import vadl.types.Type;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * The base class of all ISS (only) related expression nodes.
 * Those nodes are only used by the ISS and are required for intermediate lowering
 * for optimization purposes.
 */
public abstract class IssExprNode extends ExpressionNode {
  public IssExprNode(Type type) {
    super(type);
  }
}
