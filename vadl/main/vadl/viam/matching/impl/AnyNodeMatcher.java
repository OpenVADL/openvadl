package vadl.viam.matching.impl;

import vadl.viam.graph.Node;
import vadl.viam.matching.Matcher;

/**
 * This class always returns {@code true}.
 */
public class AnyNodeMatcher implements Matcher {
  @Override
  public boolean matches(Node node) {
    return true;
  }
}
