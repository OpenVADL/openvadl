package vadl.test.viam.algebraic_simplification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplificationPass;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplifier;
import vadl.viam.passes.canonicalization.Canonicalizer;

/**
 * This class executes all tests in the `valid_algebraic_simplification.vadl` test source.
 * This is done by searching for all function definitions starting with `exercise_`.
 * For each of these exercise functions there exists a `solution_...` function that contains
 * the expected outcome of the exercise_function.
 * Then the exercise function's behavior is the simplification using the
 * {@link AlgebraicSimplificationPass}.
 * This constant value is then compared to the constant value in the solution function.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AlgebraicSimplificationEvaluationTest extends AbstractTest {

  Specification spec;

  @Test
  public void emptyTest() {
    // just to prevent the real tests running before frontend initialization
  }

  @ParameterizedTest
  @MethodSource("constantEvalSources")
  void constantEvalTests(Function actualFunc, Function expectedFunc) {
    var algebraicSimplifier = new AlgebraicSimplifier(AlgebraicSimplificationPass.rules);
    var actual = findResult(actualFunc);
    algebraicSimplifier.run(actualFunc.behavior());
    var expected = findResult(expectedFunc);
    assertEquals(expected, actual);
  }

  Stream<Arguments> constantEvalSources() {
    spec =
        runAndGetViamSpecification("algebraicSimplification/valid_algebraic_simplification.vadl");

    System.out.println(currentTestSourceAsString());

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
    assertInstanceOf(ConstantNode.class, returnNode.value);
    var constant = ((ConstantNode) returnNode.value).constant();
    assertInstanceOf(Constant.Value.class, constant);
    return (Constant.Value) constant;
  }
}
