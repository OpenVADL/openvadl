package vadl.viam.matching;

import vadl.viam.graph.Node;
import vadl.viam.matching.impl.BuiltInMatcher;

/**
 * Interfaces to check the structure of a {@link Node}.
 */
public interface Matcher {
  /**
   * Returns {@code true} when the given {@link Node} matches
   * the conditions of the implementing class.
   */
  boolean matches(Node node);

  /**
   * Returns a new {@link Matcher} but with swapped operands.
   * This convenient when you define a {@link BuiltInMatcher} and want to
   * also allow a commutative match.
   */
  default Matcher swapOperands() {
    throw new RuntimeException("Swapping operands is not allowed");
  }
}
