// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.viam.canonicalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static vadl.TestUtils.findDefinitionByNameIn;

import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.AbstractTest;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Constant;
import vadl.viam.Function;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.passes.canonicalization.CanonicalizationPass;
import vadl.viam.passes.canonicalization.Canonicalizer;
import vadl.viam.passes.verification.ViamVerificationPass;

/**
 * This class executes all tests in the `valid_builtin_constant_eval.vadl` test source.
 * This is done by searching for all function definitions starting with `exercise_`.
 * For each of these exercise functions there exists a `solution_...` function that contains
 * the expected outcome of the exercise_function.
 * Then the exercise function's behavior is canonicalized using the {@link Canonicalizer}
 * to produce a constant value.
 * This constant value is then compared to the constant value in the solution function.
 */
public class BuiltInConstantEvaluationTest extends AbstractTest {


  @TestFactory
  Stream<DynamicTest> constantEvalTest() throws IOException, DuplicatedPassKeyException {
    var config = getConfiguration(false);
    var setup = setupPassManagerAndRunSpec(
        "passes/canonicalization/valid_builtin_constant_evaluation.vadl",
        PassOrders.viam(config)
            .untilFirst(CanonicalizationPass.class)
            .add(new ViamVerificationPass(config))
    );

    var spec = setup.specification();
    return spec.definitions()
        .filter(Function.class::isInstance)
        .map(Function.class::cast)
        .filter(f -> f.simpleName().startsWith("exercise_"))
        .map(f -> {
          var testName = f.simpleName().substring("exercise_".length());
          var solution = findDefinitionByNameIn("solution_" + testName, spec, Function.class);
          return DynamicTest.dynamicTest(testName, () -> {
            checkTestCase(f, solution);
          });
        });

  }

  void checkTestCase(Function actualFunc, Function expectedFunc) {
    var actual = findResult(actualFunc);
    var expected = findResult(expectedFunc);
    assertEquals(actual, expected);
  }


  private Constant.Value findResult(Function func) {
    assertEquals(3, func.behavior().getNodes().count(),
        "Wrong number of nodes in behavior. Test function was probably not correctly inlined.");
    var returnNode = func.behavior().getNodes(ReturnNode.class).findFirst().get();
    assertInstanceOf(ConstantNode.class, returnNode.value());
    var constant = ((ConstantNode) returnNode.value()).constant();
    assertInstanceOf(Constant.Value.class, constant);
    // cast to the expected return type of function
    System.out.println("Return type: " + func.returnType());
    System.out.println("Constant: " + constant);
    return ((Constant.Value) constant).trivialCastTo(func.returnType());
  }
}
