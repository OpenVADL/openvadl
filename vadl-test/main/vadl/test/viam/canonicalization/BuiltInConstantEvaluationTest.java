package vadl.test.viam.canonicalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vadl.test.TestUtils.findDefinitionByNameIn;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.test.AbstractTest;
import vadl.viam.Constant;
import vadl.viam.Function;
import vadl.viam.Specification;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.passes.canonicalization.Canonicalizer;

/**
 * This class executes all tests in the `valid_builtin_constant_eval.vadl` test source.
 * This is done by searching for all function definitions starting with `exercise_`.
 * For each of these exercise functions there exists a `solution_...` function that contains
 * the expected outcome of the exercise_function.
 * Then the exercise function's behavior is canonicalized using the {@link Canonicalizer}
 * to produce a constant value.
 * This constant value is then compared to the constant value in the solution function.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BuiltInConstantEvaluationTest extends AbstractTest {

  Specification spec;

  @Test
  public void emptyTest() {
    // just to prevent the real tests running before frontend initialization
  }

  @ParameterizedTest
  @MethodSource("constantEvalSources")
  void constantEvalTests(Function actualFunc, Function expectedFunc) {
    Canonicalizer.canonicalize(actualFunc.behavior());
    var actual = findResult(actualFunc);
    // expected function must be also canonicalized first
    // as it may contain a type cast
    Canonicalizer.canonicalize(expectedFunc.behavior());
    var expected = findResult(expectedFunc);
    assertEquals(expected, actual);
  }

  Stream<Arguments> constantEvalSources() {
    spec = runAndGetViamSpecification("canonicalization/valid_builtin_constant_evaluation.vadl");

    // Find all tests and corresponding solutions
    return spec.definitions()
        .filter(Function.class::isInstance)
        .map(Function.class::cast)
        .filter(f -> f.simpleName().startsWith("exercise_"))
        .map(f -> {
          var testName = f.simpleName().substring("exercise_".length());
          var solution = findDefinitionByNameIn("solution_" + testName, spec, Function.class);
          return Arguments.of(f, solution);
        });
  }


  private Constant.Value findResult(Function func) {
    assertEquals(3, func.behavior().getNodes().count(),
        "Wrong number of nodes in behavior. Test function was probably not correctly inlined.");
    var returnNode = func.behavior().getNodes(ReturnNode.class).findFirst().get();
    assertInstanceOf(ConstantNode.class, returnNode.value());
    var constant = ((ConstantNode) returnNode.value()).constant();
    assertInstanceOf(Constant.Value.class, constant);
    return (Constant.Value) constant;
  }
}
