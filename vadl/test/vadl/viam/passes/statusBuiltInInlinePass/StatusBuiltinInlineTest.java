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

package vadl.viam.passes.statusBuiltInInlinePass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static vadl.utils.GraphUtils.getSingleNode;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.types.BuiltInTable;
import vadl.types.TupleType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.TupleGetFieldNode;
import vadl.viam.passes.canonicalization.Canonicalizer;


abstract class StatusBuiltinInlineTest {

  record Test(
      Graph exercise,
      Constant.Value expectedValue
  ) {

  }

  private static final Logger log = LoggerFactory.getLogger(StatusBuiltinInlineTest.class);

  protected Stream<Test> operation(BuiltInTable.BuiltIn builtIn, List<Constant.Value> args,
                                   Constant.Value result, boolean negative, boolean zero,
                                   boolean carry,
                                   boolean overflow) {

    var argNodes = args.stream().map(Constant::toNode).map(ExpressionNode.class::cast)
        .toList();
    var builtInCall = BuiltInCall.of(builtIn, argNodes);

    var name = builtIn.name() + "_"
        + args.stream().map(Constant.Value::hexadecimal)
        .collect(Collectors.joining("_"));
    name = name.replaceFirst("VADL::", "");

    return Stream.of(
        new Test(
            buildFuncGraph(name + "__" + "result", builtInCall.copy(), -1), result),
        new Test(
            buildFuncGraph(name + "__" + "negative", builtInCall.copy(), 0), boolToBit(negative)),
        new Test(buildFuncGraph(name + "__" + "zero", builtInCall.copy(), 1), boolToBit(zero)),
        new Test(buildFuncGraph(name + "__" + "carry", builtInCall.copy(), 2), boolToBit(carry)),
        new Test(buildFuncGraph(name + "__" + "overflow", builtInCall.copy(), 3),
            boolToBit(overflow))
    );
  }

  private Constant.Value boolToBit(boolean b) {
    return Constant.Value.of(b ? 1 : 0, Type.bool());
  }

  protected Stream<DynamicTest> runTests(Test... tests) {
    return runTests(Stream.of(tests));
  }

  @SafeVarargs
  protected final Stream<DynamicTest> runTests(Stream<Test>... tests) {
    return runTests(Stream.of(tests).flatMap(s -> s));
  }

  protected Stream<DynamicTest> runTests(Stream<Test> tests) {
    return tests.map(t -> DynamicTest.dynamicTest(
        t.exercise().name,
        () -> runSingleTest(t))
    );
  }

  private void runSingleTest(Test test) {

    var graph = test.exercise();

    // run the status built-in inliner
    new StatusBuiltInInliner(graph).run();

    log.info(graph.dotGraph());

    var returnNode = getSingleNode(graph, ReturnNode.class);

    // constant evaluate function
    var result = Canonicalizer.canonicalizeSubGraph(returnNode.value());

    assertTrue("Result is not a constant value", result instanceof ConstantNode);

    var resultVal = (Constant.Value) ((ConstantNode) result).constant();
    assertEquals("Result value does not match expected value", test.expectedValue().hexadecimal(),
        resultVal.hexadecimal());
    assertEquals("Result type size does not match expected size",
        test.expectedValue().type().bitWidth(), resultVal.type().bitWidth());
  }

  private Graph buildFuncGraph(String name, ExpressionNode call, int statusIndex) {
    // check if we access exercise or status tuple
    var outerIndex = statusIndex == -1 ? 0 : 1;
    // get type of accessed tuple entry
    var outerType = ((TupleType) call.type()).get(outerIndex);
    // construct tuple getter
    var getter = new TupleGetFieldNode(
        outerIndex,
        call,
        outerType
    );

    if (statusIndex != -1) {
      // if we access a status value, we have to access the returned tuple
      getter = new TupleGetFieldNode(
          statusIndex,
          getter,
          outerType
      );
    }

    var graph = new Graph(name);
    var returnNode = graph.addWithInputs(new ReturnNode(getter));
    graph.addWithInputs(new StartNode(returnNode));
    return graph;
  }

}