package vadl.gcb.matching.impl;

import vadl.gcb.passes.encoding.nodes.NegatedNode;
import vadl.viam.graph.Node;
import vadl.viam.matching.Matcher;

/**
 * Matches a certain {@link NegatedNode}.
 */
public class NegatedNodeMatcher implements Matcher {
  private final Matcher matcher;

  public NegatedNodeMatcher(Matcher matcher) {
    this.matcher = matcher;
  }

  @Override
  public boolean matches(Node node) {
    if (node instanceof NegatedNode) {
      return matcher.matches(((NegatedNode) node).value());
    }

    return false;
  }
}
