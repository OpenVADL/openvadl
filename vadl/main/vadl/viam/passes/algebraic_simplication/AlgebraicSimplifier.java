package vadl.viam.passes.algebraic_simplication;

import java.util.List;
import java.util.Optional;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.passes.Pair;
import vadl.viam.passes.algebraic_simplication.rules.AlgebraicSimplificationRule;

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
            .map(node -> rule.simplify(node).map(y -> new Pair(node, y)))
            .flatMap(Optional::stream)
            .toList();

        for (var pair : result) {
          var oldNode = pair.oldNode();
          var newNode = pair.newNode();

          graph.replaceNode(oldNode, newNode);
          hasChanged = true;
        }
      } while (hasChanged);
    });
  }
}
