package vadl.viam.passes.algebraic_simplication.rules;

import java.math.BigInteger;
import java.util.Optional;
import vadl.gcb.passes.GenerateFieldAccessEncodingFunctionPass;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;

/**
 * - (+ a a) and
 * + (- a a) should be replaced by {@link ConstantNode} with the value {@code 0}.
 */
public class AddWithNegatedValueReductionRule implements AlgebraicSimplificationRule {
  @Override
  public Optional<Node> simplify(Node node) {
    if (node instanceof BuiltInCall) {
      var cast = (BuiltInCall) node;

      if (cast.builtIn() == BuiltInTable.ADD) {
        var arguments = cast.arguments().stream().toList();

        if (arguments
            .get(1) instanceof GenerateFieldAccessEncodingFunctionPass.NegatedNode negatedNode) {
          if (negatedNode.value().equals(arguments.get(0))) {
            return Optional.of(
                new ConstantNode(new Constant.Value(BigInteger.ZERO, (DataType) cast.type())));
          }
        }
      }
    }

    return Optional.empty();
  }
}
