package vadl.viam.passes.algebraic_simplication.rules.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static vadl.TestUtils.createIdentifier;

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

class MultiplicationWithZeroSimplificationRuleTest extends AbstractTest {
  AlgebraicSimplificationRule rule =
      new MultiplicationWithZeroSimplificationRule();

  private static Stream<Arguments> getSupportedBuiltins() {
    return Stream.of(Arguments.of(BuiltInTable.MUL), Arguments.of(BuiltInTable.MULS),
        Arguments.of(BuiltInTable.SMULL), Arguments.of(BuiltInTable.SMULLS),
        Arguments.of(BuiltInTable.UMULL), Arguments.of(BuiltInTable.SUMULL),
        Arguments.of(BuiltInTable.SUMULLS));
  }

  @ParameterizedTest
  @MethodSource("getSupportedBuiltins")
  void shouldReplaceNode(BuiltInTable.BuiltIn built) {
    var ty = Type.signedInt(32);
    var node = new BuiltInCall(built, new NodeList<>(List.of(
        new FuncParamNode(new Parameter(createIdentifier("parameterValue"), ty)),
        new ConstantNode(Constant.Value.of(0, ty)
        ))), ty);

    var res = rule.simplify(node);

    assertThat(res).isNotNull()
        .isPresent();
    assertThat(res.get()).isExactlyInstanceOf(ConstantNode.class);
    var constant = (Constant.Value) ((ConstantNode) res.get()).constant();
    assertThat(constant.intValue()).isEqualTo(0);
  }
}