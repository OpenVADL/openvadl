package vadl.viam.matching.impl;

import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.matching.Matcher;

/**
 * Checks if the node has the given {@link LetNode}.
 */
public class LetNodeMatcher implements Matcher {
  private final Matcher matcher;

  public LetNodeMatcher(Matcher matcher) {
    this.matcher = matcher;
  }


  @Override
  public boolean matches(Node node) {
    return (node instanceof LetNode letNode
        && matcher.matches(letNode.expression()));
  }
}
