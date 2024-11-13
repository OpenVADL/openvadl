package vadl.viam.passes.behaviorRewrite.rules.impl;

import java.util.Optional;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.passes.behaviorRewrite.rules.BehaviorRewriteSimplificationRule;

/**
 * Simplification rule for a graph which let nodes.
 */
public class LetNodeSimplificationRule
    implements BehaviorRewriteSimplificationRule {
  @Override
  public Optional<Node> simplify(Node node) {
    if (node instanceof LetNode n) {
      return Optional.of(n.expression());
    }

    return Optional.empty();
  }
}
