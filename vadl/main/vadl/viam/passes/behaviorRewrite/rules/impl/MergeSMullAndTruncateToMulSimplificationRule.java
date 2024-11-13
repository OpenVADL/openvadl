package vadl.viam.passes.behaviorRewrite.rules.impl;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;
import vadl.types.BuiltInTable;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.LetNodeMatcher;
import vadl.viam.matching.impl.TruncNodeMatcher;
import vadl.viam.passes.behaviorRewrite.rules.BehaviorRewriteSimplificationRule;

/**
 * Simplification rule for a graph which has a smull with a truncate node. This can be replaced
 * by the mul node.
 */
public class MergeSMullAndTruncateToMulSimplificationRule
    implements BehaviorRewriteSimplificationRule {
  @Override
  public Optional<Node> simplify(Node node) {
    if (node instanceof ExpressionNode n) {
      //TODO: find a way to make the LetNode optional.
      var matcher = new TruncNodeMatcher(new LetNodeMatcher(new BuiltInMatcher(
          BuiltInTable.SMULL, Collections.emptyList()
      )));

      var matchings = TreeMatcher.matches(Stream.of(n), matcher);
      for (var matching : matchings) {
        var casted = (TruncateNode) matching;
        var child = (LetNode) casted.value();
        var builtin = (BuiltInCall) child.expression();
        builtin.setBuiltIn(BuiltInTable.MUL);
        builtin.setType(casted.type());

        n.replaceAndDelete(builtin);
      }
    }

    return Optional.empty();
  }
}
