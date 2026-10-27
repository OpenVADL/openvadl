// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.viam.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static vadl.utils.GraphUtils.getSingleNode;

import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.viam.Constant;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.passes.canonicalization.Canonicalizer;

/**
 * Support for tests that transform a graph and check that the transformed graph
 * still evaluates to the expected value.
 * Each test case is a graph that returns a single expression.
 * The test runs the given steps (e.g., a pass or inliner) on the graph,
 * constant evaluates the returned expression using the {@link Canonicalizer}
 * and compares it with the expected value.
 */
public final class GraphEvaluationTestSupport {

  private static final Logger log = LoggerFactory.getLogger(GraphEvaluationTestSupport.class);

  private GraphEvaluationTestSupport() {
  }

  /**
   * A single test case.
   *
   * @param name          the name of the test
   * @param exercise      creates the graph to test; it is only called when the test is run
   * @param expectedValue the value the returned expression must evaluate to
   */
  public record GraphCase(
      String name,
      Supplier<Graph> exercise,
      Constant.Value expectedValue
  ) {
  }

  /**
   * A step that is applied on the graph of a test case before it is evaluated.
   * It may also contain assertions on the transformed graph.
   */
  @FunctionalInterface
  public interface GraphStep {
    void run(Graph graph) throws Exception;
  }

  /**
   * Creates a dynamic test for each case, that applies the steps in the given order
   * and checks the evaluated result.
   */
  public static Stream<DynamicTest> tests(Stream<GraphCase> cases, GraphStep... steps) {
    return cases.map(c -> DynamicTest.dynamicTest(c.name(), () -> run(c, steps)));
  }

  /**
   * Creates a graph that returns the given expression.
   */
  public static Graph returning(String name, ExpressionNode value) {
    var graph = new Graph(name);
    var returnNode = graph.addWithInputs(new ReturnNode(value));
    graph.addWithInputs(new StartNode(returnNode));
    return graph;
  }

  private static void run(GraphCase graphCase, GraphStep... steps) throws Exception {
    var graph = graphCase.exercise().get();
    for (var step : steps) {
      step.run(graph);
    }

    log.debug(graph.dotGraph());

    var returnNode = getSingleNode(graph, ReturnNode.class);

    // constant evaluate function
    var result = Canonicalizer.canonicalizeSubGraph(returnNode.value());

    assertInstanceOf(ConstantNode.class, result, "Result is not a constant value");

    var expected = graphCase.expectedValue();
    var resultVal = (Constant.Value) ((ConstantNode) result).constant();
    assertEquals(expected.hexadecimal(),
        resultVal.hexadecimal(), "Result value does not match expected value");
    assertEquals(expected.type().bitWidth(), resultVal.type().bitWidth(),
        "Result type size does not match expected size");
  }
}
