package vadl.test.viam.canonicalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vadl.test.TestUtils.findDefinitionByNameIn;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.test.AbstractTest;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Function;
import vadl.viam.Specification;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.ConstantNode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConstantEvaluationTest extends AbstractTest {

  Specification spec;
  String testISAName;

  @Test
  public void evaluateAllFormats() {
    testISAName = "ConstantArithmeticTest";
    spec = runAndGetViamSpecification("canonicalization/valid_constant_arithmetic.vadl");

    var mul_1 = findResult("solution_mul_1");
    assertEquals(Constant.Value.of(2, Type.unsignedInt(2)), mul_1);

  }

  @ParameterizedTest
  @MethodSource("constantEvalSources")
  void constantEvalTests(Function actualFunc, Function expectedFunc) {
    var actual = findResult(actualFunc);
    var expected = findResult(expectedFunc);
    assertEquals(expected, actual);
  }

  Stream<Arguments> constantEvalSources() {
    spec = runAndGetViamSpecification("canonicalization/valid_constant_arithmetic.vadl");
    testISAName = "ConstantArithmeticTest";

    return Stream.of(
        Arguments.of(
            findDefinitionByNameIn(testISAName + ".result_mul_1", spec, Function.class),
            findDefinitionByNameIn(testISAName + ".solution_mul_1", spec, Function.class)

        )
    );
  }


  private Constant.Value findResult(String functionName) {
    var func = findDefinitionByNameIn(testISAName + "." + functionName, spec, Function.class);
    return findResult(func);
  }


  private Constant.Value findResult(Function func) {
    assertEquals(3, func.behavior().getNodes().count());
    var returnNode = func.behavior().getNodes(ReturnNode.class).findFirst().get();
    assertInstanceOf(ConstantNode.class, returnNode.value);
    var constant = ((ConstantNode) returnNode.value).constant();
    assertInstanceOf(Constant.Value.class, constant);
    return (Constant.Value) constant;
  }
}
