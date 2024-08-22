package vadl.viam.matching.impl;

import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.matching.Matcher;

/**
 * Matches if the node is {@link FieldAccessRefNode}.
 */
public class FieldAccessRefMatcher implements Matcher {

  @Override
  public boolean matches(Node node) {
    return node instanceof FieldAccessRefNode;
  }
}
