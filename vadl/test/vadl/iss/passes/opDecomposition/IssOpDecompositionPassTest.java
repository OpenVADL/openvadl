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

package vadl.iss.passes.common.opDecomposition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vadl.types.BuiltInTable.BUILTIN_RESULT;
import static vadl.utils.GraphUtils.bits;
import static vadl.utils.GraphUtils.slice;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.viam.Constant;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Parameter;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FoldNode;
import vadl.viam.graph.dependency.ForIdxNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.ZeroExtendNode;
import vadl.viam.passes.canonicalization.Canonicalizer;

public class IssOpDecompositionPassTest {

  @TestFactory
  Stream<DynamicTest> lsrTests() {
    return Stream.of(
        lsrSimple(0b01110000, 1, 8, 4),
        lsrSimple(0b110100, 6, 8, 4),
        lsrSimple(0b00000001, 1, 8, 4),  // Shift LSB
        lsrSimple(0b10000000, 7, 8, 4),  // Shift MSB
        lsrSimple(0b11111111, 8, 8, 4),  // Full shift
        lsrSimple(0b00000000, 3, 8, 4),  // All zeros
        lsrSimple(0b10101010, 2, 8, 4),  // Alternating bits
        lsrSimple(0b11110000, 4, 8, 4),  // High nibble
        lsrSimple(0b00001111, 4, 8, 4),  // Low nibble
        lsrSimple(0b11111111, 0, 8, 4),  // No shift

        lsrCplx(0b01110000, 1, 8, 0b111, 6, 3, 4),
        lsrCplx(0b10110100, 3, 8, 0b0001, 7, 4, 4),
        lsrCplx(0b00000001, 1, 8, 0b0, 5, 0, 4),  // Shift LSB
        lsrCplx(0b10000000, 7, 8, 0b1, 6, 0, 4),  // Shift MSB
        lsrCplx(0b11111111, 8, 8, 0b1, 4, 4, 4),  // Full shift
        lsrCplx(0b00000000, 3, 8, 0b0, 3, 2, 4),  // All zeros
        lsrCplx(0b10101010, 2, 8, 0b1010, 6, 2, 4),  // Alternating bits
        lsrCplx(0b11110000, 4, 8, 0b1111, 3, 0, 4),  // High nibble
        lsrCplx(0b00001111, 4, 8, 0b0, 5, 0, 4),  // Low nibble
        lsrCplx(0b11111111, 0, 8, 0b1111, 6, 3, 4),   // No shift

        lsrSimple(0b110100, 6, 8, 2),
        lsrSimple(0b10000000, 7, 8, 2),  // Shift MSB
        lsrSimple(0b11111111, 8, 8, 2),  // Full shift
        lsrSimple(0b00000000, 3, 8, 2),  // All zeros
        lsrSimple(0b10101010, 2, 8, 2),  // Alternating bits
        lsrSimple(0b11110000, 4, 8, 2),  // High nibble
        lsrSimple(0b00001111, 4, 8, 2),  // Low nibble
        lsrSimple(0b11111111, 0, 8, 2),  // No shift

        lsrCplx(0b10110100, 3, 8, 0b0001, 7, 4, 2),
        lsrCplx(0b10000000, 7, 8, 0b1, 6, 0, 2),  // Shift MSB
        lsrCplx(0b11111111, 8, 8, 0b1, 4, 4, 2),  // Full shift
        lsrCplx(0b00000000, 3, 8, 0b0, 3, 2, 2),  // All zeros
        lsrCplx(0b10101010, 2, 8, 0b1010, 6, 2, 2),  // Alternating bits
        lsrCplx(0b11110000, 4, 8, 0b1111, 3, 0, 2),  // High nibble
        lsrCplx(0b00001111, 4, 8, 0b0, 5, 0, 2),  // Low nibble
        lsrCplx(0b11111111, 0, 8, 0b1111, 6, 3, 2)   // No shift
    );
  }

  @TestFactory
  Stream<DynamicTest> logicSliceTests() {
    return Stream.of(
        DynamicTest.dynamicTest("NOT_16_cross_chunk", () -> {
          var expr = BuiltInTable.NOT.call(bits(0xABCD, 16).toNode());
          testSliceAgainstCanonical(expr, 11, 4, 8);
        }),
        DynamicTest.dynamicTest("SELECT_16_cross_chunk", () -> {
          var expr = new SelectNode(
              BuiltInTable.EQU.call(bits(1, 1).toNode(), bits(1, 1).toNode()),
              bits(0xABCD, 16).toNode(),
              bits(0x1234, 16).toNode()
          );
          testSliceAgainstCanonical(expr, 11, 4, 8);
        }),
        DynamicTest.dynamicTest("CTZ_256_nonzero_low_bits", () -> {
          var value = Constant.Value.fromInteger(BigInteger.ONE.shiftLeft(130), Type.bits(256));
          var expr = BuiltInTable.CTZ.call(value.toNode());
          testSliceAgainstEvaluator(expr, 8, 0, 64);
        }),
        DynamicTest.dynamicTest("CTZ_256_zero_fallback", () -> {
          var expr = BuiltInTable.CTZ.call(Constant.Value.zero(Type.bits(256)).toNode());
          testSliceAgainstEvaluator(expr, 8, 0, 64);
        }),
        DynamicTest.dynamicTest("CLZ_256_nonzero_low_bits", () -> {
          var value = Constant.Value.fromInteger(BigInteger.ONE.shiftLeft(130), Type.bits(256));
          var expr = BuiltInTable.CLZ.call(value.toNode());
          testSliceAgainstEvaluator(expr, 8, 0, 64);
        }),
        DynamicTest.dynamicTest("CLZ_256_zero_fallback", () -> {
          var expr = BuiltInTable.CLZ.call(Constant.Value.zero(Type.bits(256)).toNode());
          testSliceAgainstEvaluator(expr, 8, 0, 64);
        }),
        DynamicTest.dynamicTest("COB_256_all_ones", () -> {
          var value = Constant.Value.fromInteger(BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE),
              Type.bits(256));
          var expr = BuiltInTable.COB.call(value.toNode());
          testSliceAgainstEvaluator(expr, 8, 0, 64);
        })
    );
  }

  @TestFactory
  Stream<DynamicTest> arithmeticSliceTests() {
    return Stream.of(
        DynamicTest.dynamicTest("ADD_16_cross_chunk_carry", () -> {
          var expr = BuiltInTable.ADD.call(bits(0x00FF, 16).toNode(), bits(0x0001, 16).toNode());
          testSliceAgainstCanonical(expr, 11, 4, 8);
        }),
        DynamicTest.dynamicTest("ADD_16_full_overflow", () -> {
          var expr = BuiltInTable.ADD.call(bits(0xFFFF, 16).toNode(), bits(0x0001, 16).toNode());
          testSliceAgainstCanonical(expr, 15, 8, 8);
        }),
        DynamicTest.dynamicTest("ADD_24_multi_chunk", () -> {
          var expr =
              BuiltInTable.ADD.call(bits(0x12FFFF, 24).toNode(), bits(0x000201, 24).toNode());
          testSliceAgainstCanonical(expr, 20, 13, 8);
        }),
        DynamicTest.dynamicTest("SUB_16_cross_chunk_borrow", () -> {
          var expr = BuiltInTable.SUB.call(bits(0x0100, 16).toNode(), bits(0x0001, 16).toNode());
          testSliceAgainstCanonical(expr, 11, 4, 8);
        }),
        DynamicTest.dynamicTest("SUB_16_full_underflow", () -> {
          var expr = BuiltInTable.SUB.call(bits(0x0000, 16).toNode(), bits(0x0001, 16).toNode());
          testSliceAgainstCanonical(expr, 15, 8, 8);
        }),
        DynamicTest.dynamicTest("SUB_24_multi_chunk", () -> {
          var expr =
              BuiltInTable.SUB.call(bits(0x120000, 24).toNode(), bits(0x000201, 24).toNode());
          testSliceAgainstCanonical(expr, 20, 13, 8);
        }),
        DynamicTest.dynamicTest("UDIV_pow2", () -> {
          var expr = BuiltInTable.UDIV.call(bits(0xABCD, 16).toNode(), bits(8, 16).toNode());
          testSliceAgainstCanonical(expr, 11, 4, 8);
        }),
        DynamicTest.dynamicTest("UDIV_after_UMOD_pow2", () -> {
          var umod = BuiltInTable.UMOD.call(bits(0xABCD, 16).toNode(), bits(64, 16).toNode());
          var expr = BuiltInTable.UDIV.call(umod, bits(8, 16).toNode());
          testSliceAgainstCanonical(expr, 6, 0, 8);
        }),
        DynamicTest.dynamicTest("UMOD_pow2_low_slice", () -> {
          var expr = BuiltInTable.UMOD.call(bits(0xABCD, 16).toNode(), bits(32, 16).toNode());
          testSliceAgainstCanonical(expr, 4, 0, 8);
        }),
        DynamicTest.dynamicTest("UMOD_pow2_high_slice_zero_fill", () -> {
          var expr = BuiltInTable.UMOD.call(bits(0xABCD, 16).toNode(), bits(32, 16).toNode());
          testSliceAgainstCanonical(expr, 7, 5, 8);
        })
    );
  }

  @TestFactory
  Stream<DynamicTest> foldSliceTests() {
    return Stream.of(
        DynamicTest.dynamicTest("FOLD_ADD_128_cross_block", () ->
            testFoldSliceAgainstExpected(BuiltInTable.ADD, 128, 0, 5, 95, 64, 64)),
        DynamicTest.dynamicTest("FOLD_ADD_128_descending", () ->
            testFoldSliceAgainstExpected(BuiltInTable.ADD, 128, 6, 2, 85, 40, 64)),
        DynamicTest.dynamicTest("FOLD_AND_128", () ->
            testFoldSliceAgainstExpected(BuiltInTable.AND, 128, 0, 4, 90, 64, 64)),
        DynamicTest.dynamicTest("FOLD_OR_128", () ->
            testFoldSliceAgainstExpected(BuiltInTable.OR, 128, 0, 4, 70, 32, 64)),
        DynamicTest.dynamicTest("FOLD_XOR_128", () ->
            testFoldSliceAgainstExpected(BuiltInTable.XOR, 128, 1, 5, 63, 16, 64)),
        DynamicTest.dynamicTest("FOLD_MUL_unsupported", () ->
            assertThatThrownBy(() -> {
              var fold = buildFoldExpr(BuiltInTable.MUL, 128, 0, 3);
              new Decomposer(64).request(fold, 90, 60);
            })
                .isInstanceOf(vadl.viam.graph.ViamGraphError.class)
                .hasMessageContaining("Unsupported fold combiner"))
    );
  }

  private DynamicTest lsrSimple(long a, int b, int width, int targetSize) {
    return DynamicTest.dynamicTest("LSR_" + a + "_" + b, () -> {
      var aN = bits(a, width).toNode();
      var bN = bits(b, width).toNode();
      var shift = BuiltInTable.LSR.call(aN, bN);
      testRequest(shift, targetSize);
    });
  }

  private DynamicTest lsrCplx(long a, int b, int width, int expected, int hi, int lo,
                              int targetSize) {
    return DynamicTest.dynamicTest("CPLX_LSR_" + a + "_" + b, () -> {
      var aN = bits(a, width).toNode();
      var bN = bits(b, width).toNode();
      var shift = BuiltInTable.LSR.call(aN, bN);
      testRequest(shift, hi, lo, expected, targetSize);
    });
  }


  private void testRequest(ExpressionNode expr, int hi, int lo, long expected, int targetSize) {
    var decomposed = new Decomposer(targetSize).request(expr.copy(), hi, lo);
    var testGraph = new Graph("test");
    testGraph.addWithInputs(decomposed);

    var decomposeResult = Canonicalizer.canonicalizeSubGraph(decomposed);

    assertThat(decomposeResult).isInstanceOf(ConstantNode.class);

    var decompVal = ((ConstantNode) decomposeResult).constant().asVal();
    assertThat(decompVal.longValue()).isEqualTo(expected);
  }

  private void testRequest(ExpressionNode expr, int targetSize) {
    var refGraph = new Graph("ref");
    var ref = refGraph.addWithInputs(expr.copy());
    var expectedResult = Canonicalizer.canonicalizeSubGraph(ref);

    assertThat(expectedResult).isInstanceOf(ConstantNode.class);

    var expectedVal = ((ConstantNode) expectedResult).constant().asVal().longValue();
    System.out.println(((ConstantNode) expectedResult).constant().asVal().binary());
    var hi = ref.type().asDataType().bitWidth() - 1;
    testRequest(ref, hi, 0, expectedVal, targetSize);
  }

  private void testSliceAgainstCanonical(ExpressionNode expr, int hi, int lo, int targetSize) {
    var refGraph = new Graph("ref");
    var refExpr = refGraph.addWithInputs(expr.copy());
    var refSlice = refGraph.addWithInputs(slice(refExpr, hi, lo));
    var expected = Canonicalizer.canonicalizeSubGraph(refSlice);
    assertThat(expected).isInstanceOf(ConstantNode.class);
    var expectedValue = ((ConstantNode) expected).constant().asVal().longValue();
    var decomposed = new Decomposer(targetSize).request(expr.copy(), hi, lo);
    assertSubgraphWithinTarget(decomposed, targetSize);
    var testGraph = new Graph("test");
    testGraph.addWithInputs(decomposed);
    var decomposeResult = Canonicalizer.canonicalizeSubGraph(decomposed);
    assertThat(decomposeResult).isInstanceOf(ConstantNode.class);
    var decompVal = ((ConstantNode) decomposeResult).constant().asVal();
    assertThat(decompVal.longValue()).isEqualTo(expectedValue);
  }

  private void testSliceAgainstEvaluator(ExpressionNode expr, int hi, int lo, int targetSize) {
    var expectedValue = evalToConstant(expr.copy())
        .slice(vadl.viam.Constant.BitSlice.of(hi, lo));
    var decomposed = new Decomposer(targetSize).request(expr.copy(), hi, lo);
    assertSubgraphWithinTarget(decomposed, targetSize);
    var actualValue = evalToConstant(decomposed);
    assertThat(actualValue.unsignedInteger()).isEqualTo(expectedValue.unsignedInteger());
  }

  private void assertSubgraphWithinTarget(ExpressionNode root, int targetSize) {
    var nodes = new ArrayList<ExpressionNode>();
    root.collectInputsWithChildren(nodes, ExpressionNode.class);
    nodes.add(root);

    for (var node : nodes) {
      if (node instanceof ConstantNode) {
        continue;
      }
      var width = node.type().asDataType().bitWidth();
      assertThat(width)
          .withFailMessage("Node exceeds target width (%d > %d): %s", width, targetSize, node)
          .isLessThanOrEqualTo(targetSize);
    }
  }

  private void testFoldSliceAgainstExpected(BuiltInTable.BuiltIn combiner,
                                            int width,
                                            int from,
                                            int to,
                                            int hi,
                                            int lo,
                                            int targetSize) {
    var fold = buildFoldExpr(combiner, width, from, to);
    var decomposed = new Decomposer(targetSize).request(fold.copy(), hi, lo);
    assertSubgraphWithinTarget(decomposed, targetSize);

    var expected = expectedFoldSlice(combiner, width, from, to, hi, lo);
    var actual = evalToConstant(decomposed).unsignedInteger();
    assertThat(actual).isEqualTo(expected);
  }

  private FoldNode buildFoldExpr(BuiltInTable.BuiltIn combiner, int width, int from, int to) {
    var idx = new ForIdxNode(Type.bits(8), from, to);
    var idxWide = GraphUtils.zeroExtend(idx, Type.bits(width).asDataType());
    var seed = bits(0x1234, width).toNode();
    var body = BuiltInTable.ADD.call(seed, idxWide);
    return new FoldNode(
        Type.bits(width).asDataType(),
        idx,
        body,
        buildCombiner(combiner, width)
    );
  }

  private Function buildCombiner(BuiltInTable.BuiltIn combiner, int width) {
    var type = Type.bits(width).asDataType();
    var left = new Parameter(Identifier.noLocation("l"), type, 0);
    var right = new Parameter(Identifier.noLocation("r"), type, 1);
    var graph = new Graph("fold_combiner_" + combiner.name().toLowerCase());
    var ret = graph.addWithInputs(new ReturnNode(combiner.call(new FuncParamNode(left),
        new FuncParamNode(right))));
    graph.addWithInputs(new StartNode(ret));
    return new Function(
        Identifier.noLocation("fold_combiner_" + combiner.name().toLowerCase()),
        new Parameter[] {left, right},
        type,
        graph
    );
  }

  private BigInteger expectedFoldSlice(BuiltInTable.BuiltIn combiner,
                                       int width,
                                       int from,
                                       int to,
                                       int hi,
                                       int lo) {
    var mask = BigInteger.ONE.shiftLeft(width).subtract(BigInteger.ONE);
    BigInteger acc;
    if (combiner == BuiltInTable.AND) {
      acc = mask;
    } else if (combiner == BuiltInTable.ADD
        || combiner == BuiltInTable.OR
        || combiner == BuiltInTable.XOR) {
      acc = BigInteger.ZERO;
    } else {
      throw new IllegalArgumentException("Unsupported combiner in test: " + combiner);
    }

    var step = from <= to ? 1 : -1;
    for (int idx = from; ; idx += step) {
      var term = BigInteger.valueOf(0x1234L + idx).and(mask);
      if (combiner == BuiltInTable.ADD) {
        acc = acc.add(term).and(mask);
      } else if (combiner == BuiltInTable.AND) {
        acc = acc.and(term);
      } else if (combiner == BuiltInTable.OR) {
        acc = acc.or(term).and(mask);
      } else if (combiner == BuiltInTable.XOR) {
        acc = acc.xor(term).and(mask);
      } else {
        throw new IllegalStateException("Unexpected combiner");
      }
      if (idx == to) {
        break;
      }
    }

    var sliceMask = BigInteger.ONE.shiftLeft(hi - lo + 1).subtract(BigInteger.ONE);
    return acc.shiftRight(lo).and(sliceMask);
  }

  private vadl.viam.Constant.Value evalToConstant(ExpressionNode expr) {
    if (expr instanceof ConstantNode c) {
      return c.constant().asVal();
    }
    if (expr instanceof BuiltInCall call) {
      var a = evalToConstant(call.arg(0));
      if (call.builtIn() == BuiltInTable.NOT) {
        return a.not();
      }
      if (call.builtIn() == BuiltInTable.CTZ) {
        var unsigned = a.unsignedInteger();
        var trailingZeros = unsigned.signum() == 0 ? a.type().bitWidth() : unsigned.getLowestSetBit();
        return Constant.Value.fromInteger(BigInteger.valueOf(trailingZeros), call.type().asDataType());
      }
      if (call.builtIn() == BuiltInTable.CLZ) {
        var unsigned = a.unsignedInteger();
        var leadingZeros = unsigned.signum() == 0 ? a.type().bitWidth() : a.type().bitWidth()
            - unsigned.bitLength();
        return Constant.Value.fromInteger(BigInteger.valueOf(leadingZeros), call.type().asDataType());
      }
      if (call.builtIn() == BuiltInTable.COB) {
        return Constant.Value.fromInteger(BigInteger.valueOf(a.unsignedInteger().bitCount()),
            call.type().asDataType());
      }
      var b = evalToConstant(call.arg(1));
      if (call.builtIn() == BuiltInTable.ADD) {
        return a.add(b, false).get(BUILTIN_RESULT, vadl.viam.Constant.Value.class);
      }
      if (call.builtIn() == BuiltInTable.AND) {
        return a.and(b);
      }
      if (call.builtIn() == BuiltInTable.OR) {
        return a.or(b);
      }
      if (call.builtIn() == BuiltInTable.XOR) {
        return a.xor(b);
      }
      if (call.builtIn() == BuiltInTable.ULTH) {
        return a.lth(b, false);
      }
      if (call.builtIn() == BuiltInTable.NEQ) {
        return Constant.Value.fromBoolean(!a.equalValue(b));
      }
      if (call.builtIn() == BuiltInTable.CONCATENATE_BITS) {
        return a.concat(b);
      }
      throw new IllegalStateException("Unsupported built-in in test evaluator: " + call.builtIn());
    }
    if (expr instanceof SliceNode sliceNode) {
      return evalToConstant(sliceNode.value()).slice(sliceNode.bitSlice());
    }
    if (expr instanceof ZeroExtendNode zeroExtendNode) {
      return evalToConstant(zeroExtendNode.value()).zeroExtend(zeroExtendNode.type().asDataType());
    }
    if (expr instanceof TruncateNode truncateNode) {
      return evalToConstant(truncateNode.value()).truncate(truncateNode.type().asDataType());
    }
    if (expr instanceof SelectNode selectNode) {
      return evalToConstant(selectNode.condition()).bool()
          ? evalToConstant(selectNode.trueCase())
          : evalToConstant(selectNode.falseCase());
    }
    throw new IllegalStateException(
        "Unsupported node in fold decomposition test evaluator: " + expr.getClass().getName());
  }

}
