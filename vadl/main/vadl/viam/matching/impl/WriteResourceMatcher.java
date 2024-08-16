package vadl.viam.matching.impl;

import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.matching.Matcher;

/**
 * Checks if the node has for the given {@link WriteResourceNode} a value which matches
 * child matcher.
 */
public class WriteResourceMatcher implements Matcher {

  private final Matcher matcher;

  public WriteResourceMatcher(Matcher matcher) {
    this.matcher = matcher;
  }


  @Override
  public boolean matches(Node node) {
    return (node instanceof WriteResourceNode write
        && matcher.matches(write.value()));
  }
}
