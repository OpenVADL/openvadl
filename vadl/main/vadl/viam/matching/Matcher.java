package vadl.viam.matching;

import vadl.viam.graph.Node;

/**
 * Interfaces to check the structure of a {@link Node}.
 */
public interface Matcher {
  /**
   * Returns {@code true} when the given {@link Node} matches
   * the conditions of the implementing class.
   */
  boolean matches(Node node);
}
