package vadl.viam.matching.impl;

import vadl.viam.Constant;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.matching.Matcher;

/**
 * Matches if the {@link Node} is a {@link Constant.Value}.
 */
public class AnyConstantValueMatcher implements Matcher {
  @Override
  public boolean matches(Node node) {
    if (node instanceof ConstantNode) {
      var cast = (ConstantNode) node;
      return cast.constant() instanceof Constant.Value;
    }
    return false;
  }
}
