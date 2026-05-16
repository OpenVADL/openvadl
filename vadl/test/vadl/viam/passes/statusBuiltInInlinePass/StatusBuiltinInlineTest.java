// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static vadl.types.BuiltInTable.BUILTIN_RESULT;
import static vadl.types.BuiltInTable.BUILTIN_STATUS;
import static vadl.types.StatusType.CARRY;
import static vadl.types.StatusType.NEGATIVE;
import static vadl.types.StatusType.OVERFLOW;
import static vadl.types.StatusType.ZERO;
import static vadl.utils.GraphUtils.getSingleNode;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.DynamicTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.types.BuiltInTable;
import vadl.types.StructType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.StructGetFieldNode;
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
            buildFuncGraph(name + "__" + "result", builtInCall.copy(), null), result),
        new Test(
            buildFuncGraph(name + "__" + "negative", builtInCall.copy(), NEGATIVE),
            boolToBit(negative)),
        new Test(buildFuncGraph(name + "__" + "zero", builtInCall.copy(), ZERO), boolToBit(zero)),
        new Test(buildFuncGraph(name + "__" + "carry", builtInCall.copy(), CARRY),
            boolToBit(carry)),
        new Test(buildFuncGraph(name + "__" + "overflow", builtInCall.copy(), OVERFLOW),
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

    assertInstanceOf(ConstantNode.class, result, "Result is not a constant value");

    var resultVal = (Constant.Value) ((ConstantNode) result).constant();
    assertEquals(test.expectedValue().hexadecimal(),
        resultVal.hexadecimal(), "Result value does not match expected value");
    assertEquals(test.expectedValue().type().bitWidth(), resultVal.type().bitWidth(),
        "Result type size does not match expected size");
  }

  private Graph buildFuncGraph(String name, ExpressionNode call, @Nullable String statusField) {
    // check if we access the result or status value
    var builtInField = statusField == null ? BUILTIN_RESULT : BUILTIN_STATUS;
    // get type of accessed struct entry
    var outerType = ((StructType) call.type()).get(builtInField);
    // construct struct getter
    var getter = new StructGetFieldNode(
        builtInField,
        call,
        outerType
    );

    if (statusField != null) {
      // if we access a status value, we have to access the returned struct
      getter = new StructGetFieldNode(
          statusField,
          getter,
          ((StructType) outerType).get(statusField)
      );
    }

    var graph = new Graph(name);
    var returnNode = graph.addWithInputs(new ReturnNode(getter));
    graph.addWithInputs(new StartNode(returnNode));
    return graph;
  }

}