package vadl.viam.matching.impl;

import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.matching.Matcher;

/**
 * Checks if the node has the given {@link TruncateNode}.
 */
public class TruncNodeMatcher implements Matcher {
  private final Matcher matcher;

  public TruncNodeMatcher(Matcher matcher) {
    this.matcher = matcher;
  }


  @Override
  public boolean matches(Node node) {
    return (node instanceof TruncateNode truncateNode
        && matcher.matches(truncateNode.value()));
  }
}
