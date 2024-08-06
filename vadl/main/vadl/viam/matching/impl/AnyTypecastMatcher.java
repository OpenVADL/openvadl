package vadl.viam.matching.impl;

import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.matching.Matcher;

/**
 * Matches a {@link TypeCastNode} but has no type restrictions.
 */
public class AnyTypecastMatcher extends TypcastMatcher {

  /**
   * Match any typecast when {@link Matcher} matches the value.
   */
  public AnyTypecastMatcher(Matcher matcher) {
    super(matcher);
  }
}
