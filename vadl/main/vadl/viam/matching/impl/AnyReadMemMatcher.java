package vadl.viam.matching.impl;

import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.matching.Matcher;

/**
 * Matches any {@link ReadMemNode}.
 */
public class AnyReadMemMatcher implements Matcher {

  @Override
  public boolean matches(Node node) {
    return node instanceof ReadMemNode;
  }
}
