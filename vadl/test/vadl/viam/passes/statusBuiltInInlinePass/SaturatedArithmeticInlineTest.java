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

package vadl.viam.passes.statusBuiltInInlinePass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.graph.Graph;
import vadl.viam.graph.GraphEvaluationTestSupport;
import vadl.viam.graph.GraphEvaluationTestSupport.GraphCase;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;

/**
 * Tests the saturated arithmetic built-ins against a reference model.
 * Small bit widths are tested exhaustively, larger ones with selected edge cases.
 * The lowering of the non-status saturated built-ins is tested by
 * {@code vadl.iss.passes.common.builtInLowering.SaturatedArithmeticLoweringTest}.
 */
public class SaturatedArithmeticInlineTest extends StatusBuiltinInlineTest {

  private static final int EXHAUSTIVE_WIDTH = 3;

  private static final long[] EDGE_CASES_32 = {
      0x0L, 0x1L, 0x2L, 0x7FFFFFFEL, 0x7FFFFFFFL, 0x80000000L, 0x80000001L, 0xFFFFFFFEL,
      0xFFFFFFFFL, 0x12345678L, 0x9ABCDEF0L
  };

  /**
   * The kind of the wrapping operation that is saturated.
   */
  enum Kind {
    // a + b
    ADD,
    // a + b + c
    ADD_CARRY,
    // a - b, carry = no borrow
    SUB,
    // a - b - !c, carry = no borrow
    SUB_CARRY,
    // a - b - c, carry = borrow
    SUB_BORROW
  }

  record Op(BuiltInTable.BuiltIn builtIn, boolean signed, Kind kind) {
    boolean hasCarryIn() {
      return kind == Kind.ADD_CARRY || kind == Kind.SUB_CARRY || kind == Kind.SUB_BORROW;
    }
  }

  private static final List<Op> STATUS_OPS = List.of(
      new Op(BuiltInTable.SSATADDS, true, Kind.ADD),
      new Op(BuiltInTable.USATADDS, false, Kind.ADD),
      new Op(BuiltInTable.SSATADDC, true, Kind.ADD_CARRY),
      new Op(BuiltInTable.USATADDC, false, Kind.ADD_CARRY),
      new Op(BuiltInTable.SSATSUBS, true, Kind.SUB),
      new Op(BuiltInTable.USATSUBS, false, Kind.SUB),
      new Op(BuiltInTable.SSATSUBC, true, Kind.SUB_CARRY),
      new Op(BuiltInTable.USATSUBC, false, Kind.SUB_CARRY),
      new Op(BuiltInTable.SSATSUBB, true, Kind.SUB_BORROW),
      new Op(BuiltInTable.USATSUBB, false, Kind.SUB_BORROW)
  );

  private static final Map<BuiltInTable.BuiltIn, Op> PLAIN = Map.of(
      BuiltInTable.SSATADD, new Op(BuiltInTable.SSATADD, true, Kind.ADD),
      BuiltInTable.USATADD, new Op(BuiltInTable.USATADD, false, Kind.ADD),
      BuiltInTable.SSATSUB, new Op(BuiltInTable.SSATSUB, true, Kind.SUB),
      BuiltInTable.USATSUB, new Op(BuiltInTable.USATSUB, false, Kind.SUB)
  );

  record Expected(BigInteger result, boolean negative, boolean zero, boolean carry,
                  boolean overflow) {
  }

  /**
   * Computes the expected result and flags of a saturated operation.
   * {@code a} and {@code b} are given as raw bit patterns.
   */
  private static Expected reference(Op op, int width, long a, long b, boolean c) {
    var mod = BigInteger.ONE.shiftLeft(width);
    var ua = BigInteger.valueOf(a);
    var ub = BigInteger.valueOf(b);
    var x = op.signed() ? toSigned(ua, width) : ua;
    var y = op.signed() ? toSigned(ub, width) : ub;
    var ci = BigInteger.valueOf(c ? 1 : 0);

    // exact result and the unsigned exact result (for the carry flag)
    BigInteger exact;
    BigInteger unsignedExact;
    switch (op.kind()) {
      case ADD -> {
        exact = x.add(y);
        unsignedExact = ua.add(ub);
      }
      case ADD_CARRY -> {
        exact = x.add(y).add(ci);
        unsignedExact = ua.add(ub).add(ci);
      }
      case SUB -> {
        exact = x.subtract(y);
        unsignedExact = ua.subtract(ub);
      }
      case SUB_CARRY -> {
        exact = x.subtract(y).subtract(BigInteger.ONE.subtract(ci));
        unsignedExact = ua.subtract(ub).subtract(BigInteger.ONE.subtract(ci));
      }
      case SUB_BORROW -> {
        exact = x.subtract(y).subtract(ci);
        unsignedExact = ua.subtract(ub).subtract(ci);
      }
      default -> throw new IllegalStateException();
    }

    var min = op.signed() ? BigInteger.ONE.shiftLeft(width - 1).negate() : BigInteger.ZERO;
    var max = op.signed()
        ? BigInteger.ONE.shiftLeft(width - 1).subtract(BigInteger.ONE)
        : mod.subtract(BigInteger.ONE);
    var saturated = exact.max(min).min(max);
    var result = saturated.mod(mod);

    var carry = switch (op.kind()) {
      case ADD, ADD_CARRY -> unsignedExact.compareTo(mod) >= 0;
      case SUB, SUB_CARRY -> unsignedExact.signum() >= 0;
      case SUB_BORROW -> unsignedExact.signum() < 0;
    };

    return new Expected(
        result,
        result.testBit(width - 1),
        result.signum() == 0,
        carry,
        !saturated.equals(exact)
    );
  }

  private static BigInteger toSigned(BigInteger value, int width) {
    return value.testBit(width - 1) ? value.subtract(BigInteger.ONE.shiftLeft(width)) : value;
  }

  private static BitsType operandType(Op op, int width) {
    return op.signed() ? Type.signedInt(width) : Type.unsignedInt(width);
  }

  private static Constant.Value operand(Op op, int width, long raw) {
    var value = BigInteger.valueOf(raw);
    return Constant.Value.fromInteger(op.signed() ? toSigned(value, width) : value,
        operandType(op, width));
  }

  private static List<long[]> exhaustiveOperands() {
    var operands = new ArrayList<long[]>();
    var max = 1L << EXHAUSTIVE_WIDTH;
    for (long a = 0; a < max; a++) {
      for (long b = 0; b < max; b++) {
        operands.add(new long[] {a, b});
      }
    }
    return operands;
  }

  private static List<long[]> edgeCaseOperands() {
    var operands = new ArrayList<long[]>();
    for (var a : EDGE_CASES_32) {
      for (var b : EDGE_CASES_32) {
        operands.add(new long[] {a, b});
      }
    }
    return operands;
  }

  private Stream<GraphCase> statusTests(Op op, int width, List<long[]> operands) {
    var carryIns = op.hasCarryIn() ? List.of(false, true) : List.of(false);
    return operands.stream().flatMap(ops -> carryIns.stream().flatMap(c -> {
      var args = new ArrayList<Constant.Value>();
      args.add(operand(op, width, ops[0]));
      args.add(operand(op, width, ops[1]));
      if (op.hasCarryIn()) {
        args.add(Constant.Value.of(c));
      }
      var expected = reference(op, width, ops[0], ops[1], c);
      return operation(op.builtIn(), args,
          Constant.Value.fromInteger(expected.result(), Type.bits(width)),
          expected.negative(), expected.zero(), expected.carry(), expected.overflow());
    }));
  }

  @TestFactory
  Stream<DynamicTest> exhaustiveStatusTests() {
    var operands = exhaustiveOperands();
    return runSaturatedTests(STATUS_OPS.stream()
        .flatMap(op -> statusTests(op, EXHAUSTIVE_WIDTH, operands)));
  }

  @TestFactory
  Stream<DynamicTest> edgeCaseStatusTests() {
    var operands = edgeCaseOperands();
    return runSaturatedTests(STATUS_OPS.stream()
        .flatMap(op -> statusTests(op, 32, operands)));
  }

  /**
   * Runs the status built-in inliner and evaluates the resulting graph.
   * The non-status saturated built-ins that remain in the graph cannot be constant evaluated,
   * so we replace them by the value of the reference model.
   */
  private Stream<DynamicTest> runSaturatedTests(Stream<GraphCase> cases) {
    return GraphEvaluationTestSupport.tests(cases,
        StatusBuiltinInlineTest::inline,
        SaturatedArithmeticInlineTest::assertInlined,
        SaturatedArithmeticInlineTest::evaluatePlainBuiltIns
    );
  }

  private static void assertInlined(Graph graph) {
    assertTrue(graph.getNodes(BuiltInCall.class)
            .noneMatch(c -> c.builtIn().isStatusBuiltin()),
        "Status built-in was not inlined");
  }

  private static void evaluatePlainBuiltIns(Graph graph) {
    graph.getNodes(BuiltInCall.class)
        .filter(c -> PLAIN.containsKey(c.builtIn()))
        .toList()
        .forEach(SaturatedArithmeticInlineTest::evaluatePlain);
  }

  private static void evaluatePlain(BuiltInCall call) {
    var op = PLAIN.get(call.builtIn());
    var width = call.arg(0).type().asDataType().bitWidth();
    var a = ((Constant.Value) ((ConstantNode) call.arg(0)).constant()).unsignedInteger();
    var b = ((Constant.Value) ((ConstantNode) call.arg(1)).constant()).unsignedInteger();
    var expected = reference(op, width, a.longValue(), b.longValue(), false).result();
    var type = operandType(op, width);
    assertEquals(type, call.type(), "Non-status built-in must have the SInt/UInt result type");
    var value = op.signed() ? toSigned(expected, width) : expected;
    call.replaceAndDelete(Constant.Value.fromInteger(value, type).toNode());
  }

  @TestFactory
  Stream<DynamicTest> nonStatusBuiltInIsUsedForResult() {
    // s/usatadds and s/usatsubs must keep the non-status built-in for the result,
    // so that each backend can lower the saturation on its own.
    return STATUS_OPS.stream()
        .filter(op -> !op.hasCarryIn())
        .map(op -> DynamicTest.dynamicTest(op.builtIn().name(), () -> {
          var graphCase = statusTests(op, 32, List.<long[]>of(new long[] {1, 2}))
              .findFirst().orElseThrow();
          var graph = graphCase.exercise().get();
          inline(graph);
          assertTrue(graph.getNodes(BuiltInCall.class)
                  .anyMatch(c -> PLAIN.containsKey(c.builtIn())),
              "Result does not use the non-status saturated built-in");
        }));
  }
}
