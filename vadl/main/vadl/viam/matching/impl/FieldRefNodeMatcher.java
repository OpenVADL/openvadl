package vadl.viam.matching.impl;

import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.matching.Matcher;

/**
 * Matches if the node is {@link FieldRefNode}.
 */
public class FieldRefNodeMatcher implements Matcher {

  @Override
  public boolean matches(Node node) {
    return node instanceof FieldRefNode;
  }
}
