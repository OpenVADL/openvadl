package vadl.viam.matching.impl;

import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.matching.Matcher;

/**
 * Checks if the node has for the given {@link WriteResourceNode} an address which matches
 * child matcher.
 */
public class WriteResourceMatcherForAddr implements Matcher {

  private final Matcher matcher;

  public WriteResourceMatcherForAddr(Matcher matcher) {
    this.matcher = matcher;
  }


  @Override
  public boolean matches(Node node) {
    return (node instanceof WriteResourceNode write
        && write.address() != null
        && matcher.matches(write.address()));
  }
}
