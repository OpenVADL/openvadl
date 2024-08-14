package vadl.viam.passes.algebraic_simplication.rules.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import vadl.types.BuiltInTable;
import vadl.viam.Constant;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.AnyNodeMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.ConstantValueMatcher;
import vadl.viam.passes.algebraic_simplication.rules.AlgebraicSimplificationRule;

/**
 * Simplification rule when OR with {@code false} then return the first operand of the OR.
 */
public class OrWithFalseSimplificationRule implements AlgebraicSimplificationRule {
  @Override
  public Optional<Node> simplify(Node node) {
    if (node instanceof ExpressionNode n) {
      var matcher =
          new BuiltInMatcher(List.of(BuiltInTable.OR, BuiltInTable.ORS),
              List.of(new AnyNodeMatcher(), new ConstantValueMatcher(
                  Constant.Value.of(false))));

      var matchings = TreeMatcher.matches(Stream.of(node), matcher);
      if (!matchings.isEmpty()) {
        return Optional.ofNullable(n.inputs().toList().get(0));
      }
    }
    return Optional.empty();
  }
}
