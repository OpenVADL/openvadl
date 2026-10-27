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

import static vadl.types.BuiltInTable.BUILTIN_RESULT;
import static vadl.types.BuiltInTable.BUILTIN_STATUS;
import static vadl.types.StatusType.CARRY;
import static vadl.types.StatusType.NEGATIVE;
import static vadl.types.StatusType.OVERFLOW;
import static vadl.types.StatusType.ZERO;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.DynamicTest;
import vadl.types.BuiltInTable;
import vadl.types.StructType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphEvaluationTestSupport;
import vadl.viam.graph.GraphEvaluationTestSupport.GraphCase;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.StructGetFieldNode;


abstract class StatusBuiltinInlineTest {

  /**
   * Runs the status built-in inliner on the graph.
   */
  protected static void inline(Graph graph) {
    new StatusBuiltInInliner(graph).run();
  }

  protected Stream<GraphCase> operation(BuiltInTable.BuiltIn builtIn, List<Constant.Value> args,
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
        graphCase(name, builtInCall, null, result),
        graphCase(name, builtInCall, NEGATIVE, boolToBit(negative)),
        graphCase(name, builtInCall, ZERO, boolToBit(zero)),
        graphCase(name, builtInCall, CARRY, boolToBit(carry)),
        graphCase(name, builtInCall, OVERFLOW, boolToBit(overflow))
    );
  }

  private GraphCase graphCase(String name, BuiltInCall builtInCall, @Nullable String statusField,
                              Constant.Value expected) {
    var caseName = name + "__" + (statusField == null ? "result" : statusField);
    return new GraphCase(caseName,
        () -> buildFuncGraph(caseName, builtInCall.copy(), statusField), expected);
  }

  private Constant.Value boolToBit(boolean b) {
    return Constant.Value.of(b ? 1 : 0, Type.bool());
  }

  protected Stream<DynamicTest> runTests(GraphCase... tests) {
    return runTests(Stream.of(tests));
  }

  @SafeVarargs
  protected final Stream<DynamicTest> runTests(Stream<GraphCase>... tests) {
    return runTests(Stream.of(tests).flatMap(s -> s));
  }

  protected Stream<DynamicTest> runTests(Stream<GraphCase> tests) {
    return GraphEvaluationTestSupport.tests(tests, StatusBuiltinInlineTest::inline);
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

    return GraphEvaluationTestSupport.returning(name, getter);
  }

}