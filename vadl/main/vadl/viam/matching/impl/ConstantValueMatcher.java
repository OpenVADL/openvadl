package vadl.viam.matching.impl;

import vadl.viam.Constant;
import vadl.viam.Constant.Value;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;

/**
 * Matches a certain {@link Value}.
 */
public class ConstantValueMatcher extends AnyConstantValueMatcher {
  private final Constant constant;

  public ConstantValueMatcher(Constant.Value constant) {
    this.constant = constant;
  }

  @Override
  public boolean matches(Node node) {
    var isConstantValue = super.matches(node);

    if (isConstantValue) {
      return ((ConstantNode) node).constant.equals(constant);
    }

    return false;
  }
}
