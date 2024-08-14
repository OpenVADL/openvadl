package vadl.viam.passes.algebraic_simplication.rules.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.AbstractTest;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Parameter;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.passes.algebraic_simplication.rules.AlgebraicSimplificationRule;

class AndWithTrueSimplificationRuleTest extends AbstractTest {
  AlgebraicSimplificationRule rule =
      new AndWithTrueSimplificationRule();

  private static Stream<Arguments> getSupportedBuiltins() {
    return Stream.of(Arguments.of(BuiltInTable.AND), Arguments.of(BuiltInTable.ANDS));
  }

  @ParameterizedTest
  @MethodSource("getSupportedBuiltins")
  void shouldReplaceNode(BuiltInTable.BuiltIn built) {
    var ty = Type.bool();
    var node = new BuiltInCall(built, new NodeList<>(List.of(
        new FuncParamNode(new Parameter(createIdentifier("parameterValue"), ty)),
        new ConstantNode(Constant.Value.of(true)
        ))), ty);

    var res = rule.simplify(node);

    assertThat(res).isNotNull()
        .isPresent();
    assertThat(res.get()).isExactlyInstanceOf(FuncParamNode.class);
  }
}