package vadl.viam.passes.algebraic_simplication;

import java.util.List;
import java.util.Optional;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.passes.Pair;
import vadl.viam.passes.algebraic_simplication.rules.AlgebraicSimplificationRule;

/**
 * This class contains the main driver logic to simplify algebraic expressions.
 * When instantiating a new object, the user can give a list of rules which should be applied on
 * each node.
 * Usually, this list will be a static list in the {@link AlgebraicSimplificationPass}. However,
 * it might be the case that the lcb (or others) requires special nodes. These rules with
 * non VIAM nodes should not be applied when running the {@link AlgebraicSimplificationPass}.
 * The passes with special requirements can individually define which
 * {@link AlgebraicSimplificationRule} applies.
 */
public class AlgebraicSimplifier {
  private final List<AlgebraicSimplificationRule> rules;

  public AlgebraicSimplifier(List<AlgebraicSimplificationRule> rules) {
    this.rules = rules;
  }

  /**
   * Apply algebraic simplification as long as something changes on the given {@link Graph}.
   *
   * @param graph where the simplification should be applied on.
   */
  public void run(Graph graph) {
    rules.forEach(rule -> {
      boolean hasChanged;

      do {
        hasChanged = false;

        var result = graph.getNodes()
            .filter(Node::isActive)
            // When `normalize` returns an Optional
            // then create a `Pair`
            .map(node -> rule.simplify(node).map(y -> new Pair<>(node, y)))
            .flatMap(Optional::stream)
            .toList();

        for (var pair : result) {
          var oldNode = pair.left();
          var newNode = pair.right();

          oldNode.replaceAndDelete(newNode);
          hasChanged = true;
        }
      } while (hasChanged);
    });
  }
}
