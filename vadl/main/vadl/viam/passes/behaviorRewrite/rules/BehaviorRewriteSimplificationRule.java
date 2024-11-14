package vadl.viam.passes.behaviorRewrite.rules;

import java.util.Optional;
import vadl.viam.graph.Node;

/**
 * Represents that a class implements a behavior rewrite simplification.
 */
public interface BehaviorRewriteSimplificationRule {
  /**
   * Check and apply an algebraic simplification.
   *
   * @param node is {@link Node} where the check is applied on.
   * @return {@link Optional} when it can be replaced and {@code empty} when not.
   */
  Optional<Node> simplify(Node node);
}
