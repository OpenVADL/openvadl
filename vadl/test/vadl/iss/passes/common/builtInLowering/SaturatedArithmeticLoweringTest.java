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

package vadl.iss.passes.common.builtInLowering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vadl.utils.GraphUtils.getSingleNode;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphEvaluationTestSupport;
import vadl.viam.graph.GraphEvaluationTestSupport.GraphCase;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;

/**
 * Tests the lowering of the non-status saturated built-ins against a reference model.
 * Small bit widths are tested exhaustively, larger ones with selected edge cases.
 */
public class SaturatedArithmeticLoweringTest {

  private static final int EXHAUSTIVE_WIDTH = 3;

  private static final long[] EDGE_CASES_32 = {
      0x0L, 0x1L, 0x2L, 0x7FFFFFFEL, 0x7FFFFFFFL, 0x80000000L, 0x80000001L, 0xFFFFFFFEL,
      0xFFFFFFFFL, 0x12345678L, 0x9ABCDEF0L
  };

  record Op(BuiltInTable.BuiltIn builtIn, boolean signed, boolean add) {
  }

  private static final List<Op> OPS = List.of(
      new Op(BuiltInTable.SSATADD, true, true),
      new Op(BuiltInTable.USATADD, false, true),
      new Op(BuiltInTable.SSATSUB, true, false),
      new Op(BuiltInTable.USATSUB, false, false)
  );

  /**
   * Computes the expected raw bit pattern of a saturated operation.
   */
  private static BigInteger reference(Op op, int width, long a, long b) {
    var mod = BigInteger.ONE.shiftLeft(width);
    var x = op.signed() ? toSigned(BigInteger.valueOf(a), width) : BigInteger.valueOf(a);
    var y = op.signed() ? toSigned(BigInteger.valueOf(b), width) : BigInteger.valueOf(b);
    var exact = op.add() ? x.add(y) : x.subtract(y);
    var min = op.signed() ? BigInteger.ONE.shiftLeft(width - 1).negate() : BigInteger.ZERO;
    var max = op.signed()
        ? BigInteger.ONE.shiftLeft(width - 1).subtract(BigInteger.ONE)
        : mod.subtract(BigInteger.ONE);
    return exact.max(min).min(max).mod(mod);
  }

  private static BigInteger toSigned(BigInteger value, int width) {
    return value.testBit(width - 1) ? value.subtract(BigInteger.ONE.shiftLeft(width)) : value;
  }

  private static Stream<GraphCase> cases(Op op, int width, List<long[]> operands) {
    return operands.stream().map(ops -> {
      // earlier passes may remove the casts to SInt/UInt, so the operands are plain bits
      var a = Constant.Value.of(ops[0], Type.bits(width));
      var b = Constant.Value.of(ops[1], Type.bits(width));
      var expected = reference(op, width, ops[0], ops[1]);
      var name = op.builtIn().name().replaceFirst("VADL::", "")
          + "_" + a.hexadecimal() + "_" + b.hexadecimal();
      return new GraphCase(name,
          () -> GraphEvaluationTestSupport.returning(name,
              BuiltInCall.of(op.builtIn(), a.toNode(), b.toNode())),
          Constant.Value.fromInteger(expected, Type.bits(width)));
    });
  }

  private static Stream<DynamicTest> tests(Op op, int width, List<long[]> operands) {
    var expectedType = op.signed() ? Type.signedInt(width) : Type.unsignedInt(width);
    return GraphEvaluationTestSupport.tests(cases(op, width, operands),
        SaturatedArithmeticLoweringTest::lower,
        graph -> {
          assertTrue(graph.getNodes(BuiltInCall.class)
                  .noneMatch(c -> c.builtIn() == op.builtIn()),
              "Saturated built-in was not lowered");
          assertEquals(expectedType, getSingleNode(graph, ReturnNode.class).value().type(),
              "Lowered result must have the SInt/UInt type of the built-in");
        }
    );
  }

  private static void lower(Graph graph) {
    var lowerer = new BuiltInLowerer();
    graph.getNodes(BuiltInCall.class)
        .toList()
        .forEach(call -> lowerer.dispatch(call, call.builtIn()));
  }

  @TestFactory
  Stream<DynamicTest> exhaustiveTests() {
    var operands = new ArrayList<long[]>();
    var max = 1L << EXHAUSTIVE_WIDTH;
    for (long a = 0; a < max; a++) {
      for (long b = 0; b < max; b++) {
        operands.add(new long[] {a, b});
      }
    }
    return OPS.stream().flatMap(op -> tests(op, EXHAUSTIVE_WIDTH, operands));
  }

  @TestFactory
  Stream<DynamicTest> edgeCaseTests() {
    var operands = new ArrayList<long[]>();
    for (var a : EDGE_CASES_32) {
      for (var b : EDGE_CASES_32) {
        operands.add(new long[] {a, b});
      }
    }
    return OPS.stream().flatMap(op -> tests(op, 32, operands));
  }
}
