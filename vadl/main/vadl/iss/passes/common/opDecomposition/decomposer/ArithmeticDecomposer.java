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

package vadl.iss.passes.common.opDecomposition.decomposer;

import static vadl.types.BuiltInTable.LSL;
import static vadl.types.BuiltInTable.MUL;
import static vadl.types.BuiltInTable.SMULL;
import static vadl.types.BuiltInTable.SUMULL;

import java.math.BigInteger;
import java.util.List;
import vadl.iss.passes.common.opDecomposition.nodes.IssMul2Node;
import vadl.iss.passes.common.opDecomposition.nodes.IssMulKind;
import vadl.iss.passes.common.opDecomposition.nodes.IssMulhNode;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.viam.Constant;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.StructGetFieldNode;

/**
 * Decomposes arithmetic operations (SUB) that require carry/borrow handling.
 *
 * <p>Unlike bitwise operations, arithmetic operations cannot be simply decomposed
 * slice by slice because of carry/borrow propagation. This decomposer handles
 * this by computing from bit 0 up to the requested MSB, then extracting the slice.
 */
public interface ArithmeticDecomposer extends IDecomposer {

  /**
   * Decomposes unsigned division for a requested bit slice.
   *
   * <p>Currently supports only divisors that are positive powers of two.
   */
  default ExpressionNode udivDecompose(BuiltInCall src, int hi, int lo) {
    src.ensure(src.builtIn() == BuiltInTable.UDIV, "Not an UDIV built-in call");
    var dividend = src.arg(0);
    var divisor = src.arg(1);

    if (!(divisor instanceof ConstantNode divisorConst)) {
      throw new IllegalStateException("Not yet implemented: " + src);
    }

    BigInteger divValue = divisorConst.constant().asVal().unsignedInteger();
    if (!isPowerOfTwoPositive(divValue)) {
      throw new IllegalStateException("Not yet implemented: " + src);
    }

    int shift = divValue.getLowestSetBit();
    int srcLo = lo + shift;
    int srcHi = hi + shift;

    ExpressionNode source = dividend;
    int sourceWidth = source.type().asDataType().bitWidth();
    if (dividend instanceof BuiltInCall modCall
        && modCall.builtIn() == BuiltInTable.UMOD
        && modCall.arg(1) instanceof ConstantNode modConst) {
      BigInteger modValue = modConst.constant().asVal().unsignedInteger();
      if (isPowerOfTwoPositive(modValue)) {
        source = modCall.arg(0);
        sourceWidth = Math.min(source.type().asDataType().bitWidth(), modValue.getLowestSetBit());
      }
    }

    int outWidth = hi - lo + 1;
    if (srcLo >= sourceWidth) {
      return Constant.Value.zero(Type.bits(outWidth)).toNode();
    }

    srcHi = Math.min(srcHi, sourceWidth - 1);
    var sliced = source.type().asDataType().bitWidth() <= targetSize()
        ? GraphUtils.slice(source, srcHi, srcLo)
        : request(source, srcHi, srcLo);

    return sliced.type().asDataType().bitWidth() == outWidth
        ? sliced
        : GraphUtils.zeroExtend(sliced, Type.bits(outWidth));
  }

  /**
   * Decomposes unsigned modulo for a requested bit slice.
   *
   * <p>Currently supports only divisors that are positive powers of two.
   */
  default ExpressionNode umodDecompose(BuiltInCall src, int hi, int lo) {
    src.ensure(src.builtIn() == BuiltInTable.UMOD, "Not an UMOD built-in call");
    var divisor = src.arg(1);

    if (!(divisor instanceof ConstantNode divisorConst)) {
      throw new IllegalStateException("Not yet implemented: " + src);
    }

    BigInteger modValue = divisorConst.constant().asVal().unsignedInteger();
    if (!isPowerOfTwoPositive(modValue)) {
      throw new IllegalStateException("Not yet implemented: " + src);
    }

    int keptBits = modValue.getLowestSetBit();
    int outWidth = hi - lo + 1;
    if (lo >= keptBits) {
      return Constant.Value.zero(Type.bits(outWidth)).toNode();
    }

    int srcLo = lo;
    int srcHi = Math.min(hi, keptBits - 1);
    var dividend = src.arg(0);
    var sliced = request(dividend, srcHi, srcLo);
    return sliced.type().asDataType().bitWidth() == outWidth
        ? sliced
        : GraphUtils.zeroExtend(sliced, Type.bits(outWidth));
  }

  private static boolean isPowerOfTwoPositive(BigInteger value) {
    return value.signum() > 0
        && value.and(value.subtract(BigInteger.ONE)).equals(BigInteger.ZERO);
  }

  /**
   * Decomposes an addition call to extract only the bit-range [hi:lo] of the result.
   *
   * <p>For {@code a + b}, carry propagates from lower bits to higher bits. This implementation
   * computes chunk-wise from bit 0 up to {@code hi} using only <= target-size operations.
   *
   * @param src built-in ADD call {@code ADD(a,b)}
   * @param hi  most-significant bit of the slice (inclusive, 0 = LSB)
   * @param lo  least-significant bit of the slice
   * @return graph expression that equals {@code (a + b)[hi:lo]}
   */
  default ExpressionNode addDecompose(BuiltInCall src, int hi, int lo) {
    src.ensure(src.builtIn() == BuiltInTable.ADD, "Not an ADD built-in call");
    src.ensure(hi >= lo, "Expected hi >= lo");

    var a = src.arg(0);
    var b = src.arg(1);
    int chunkSize = targetSize();
    int lastChunk = hi / chunkSize;

    ExpressionNode carryIn = Constant.Value.zero(Type.bool()).toNode();
    ExpressionNode result = null;

    for (int chunk = 0; chunk <= lastChunk; chunk++) {
      int chunkLo = chunk * chunkSize;
      int chunkHi = Math.min(chunkLo + chunkSize - 1, hi);
      int chunkWidth = chunkHi - chunkLo + 1;
      var chunkType = Type.bits(chunkWidth);

      var leftChunk = request(a, chunkHi, chunkLo);
      var rightChunk = request(b, chunkHi, chunkLo);
      var carryWord = boolToWord(carryIn, chunkType);

      var sum0 = BuiltInTable.ADD.call(leftChunk, rightChunk);
      var sum = BuiltInTable.ADD.call(sum0, carryWord);

      var carry0 = BuiltInTable.ULTH.call(sum0, leftChunk);
      var carry1 = BuiltInTable.ULTH.call(sum, sum0);
      carryIn = GraphUtils.or(carry0, carry1);

      if (chunkHi >= lo) {
        int partLo = Math.max(lo, chunkLo) - chunkLo;
        int partHi = chunkWidth - 1;
        var part = GraphUtils.slice(sum, partHi, partLo);
        result = result == null ? part : GraphUtils.concat(part, result);
      }
    }

    src.ensure(result != null, "No result generated for ADD decomposition");
    return result;
  }

  /**
   * Decomposes a subtraction call to extract only the bit-range [hi:lo] of the result.
   *
   * <p>For {@code a - b}, borrow propagates from lower bits to higher bits. This implementation
   * computes chunk-wise from bit 0 up to {@code hi} using only <= target-size operations.
   *
   * @param src built-in SUB call {@code SUB(a,b)}
   * @param hi  most-significant bit of the slice (inclusive, 0 = LSB)
   * @param lo  least-significant bit of the slice
   * @return graph expression that equals {@code (a - b)[hi:lo]}
   */
  default ExpressionNode subDecompose(BuiltInCall src, int hi, int lo) {
    src.ensure(src.builtIn() == BuiltInTable.SUB, "Not a SUB built-in call");
    src.ensure(hi >= lo, "Expected hi >= lo");

    var a = src.arg(0);
    var b = src.arg(1);
    int chunkSize = targetSize();
    int lastChunk = hi / chunkSize;

    ExpressionNode borrowIn = Constant.Value.zero(Type.bool()).toNode();
    ExpressionNode result = null;

    for (int chunk = 0; chunk <= lastChunk; chunk++) {
      int chunkLo = chunk * chunkSize;
      int chunkHi = Math.min(chunkLo + chunkSize - 1, hi);
      int chunkWidth = chunkHi - chunkLo + 1;
      var chunkType = Type.bits(chunkWidth);

      var leftChunk = request(a, chunkHi, chunkLo);
      var rightChunk = request(b, chunkHi, chunkLo);
      var borrowWord = boolToWord(borrowIn, chunkType);

      var diff0 = BuiltInTable.SUB.call(leftChunk, rightChunk);
      var diff = BuiltInTable.SUB.call(diff0, borrowWord);

      var borrow0 = BuiltInTable.ULTH.call(leftChunk, rightChunk);
      var borrow1 = BuiltInTable.ULTH.call(diff0, borrowWord);
      borrowIn = GraphUtils.or(borrow0, borrow1);

      if (chunkHi >= lo) {
        int partLo = Math.max(lo, chunkLo) - chunkLo;
        int partHi = chunkWidth - 1;
        var part = GraphUtils.slice(diff, partHi, partLo);
        result = result == null ? part : GraphUtils.concat(part, result);
      }
    }

    src.ensure(result != null, "No result generated for SUB decomposition");
    return result;
  }

  /**
   * Decomposes a long multiplication call to extract only the bit-range [hi:lo] of the result.
   *
   * <p>The original result bit width of the operation may not be twice as large as the target
   * size, as that would imply that the input operands are larger than the target size, which
   * cannot be decomposed. Also, the bit width of the requested slice may not exceed the target
   * size.
   *
   * <p>If the slice covers both the high and low part of the result, the long multiplication is
   * decomposed into a {@link IssMul2Node}. Otherwise, it is either replaced by a
   * {@link IssMulhNode} or a call to {@link BuiltInTable#MUL}.
   *
   * @param src built-in UMULL, SMULL or SUMULL call
   * @param hi  most-significant bit of the slice (inclusive, 0 = LSB)
   * @param lo  least-significant bit of the slice
   * @return graph expression that equals {@code (a * b)[hi:lo]}
   */
  default ExpressionNode mullDecompose(BuiltInCall src, int hi, int lo) {
    src.ensure(
        List.of(BuiltInTable.SMULL, BuiltInTable.UMULL, BuiltInTable.SUMULL)
            .contains(src.builtIn()),
        "Not a UMULL, SMULL or SUMULL built-in call"
    );
    src.ensure(hi >= lo, "Expected hi >= lo");
    checkMullSlice(src, hi, lo);

    var kind = src.builtIn() == SUMULL
        ? IssMulKind.SIGNED_UNSIGNED
        : src.builtIn() == SMULL
          ? IssMulKind.SIGNED_SIGNED
          : IssMulKind.UNSIGNED_UNSIGNED;

    if (hi < targetSize() || lo >= targetSize()) {
      // the slice does not use parts from both, the upper half and lower half.
      return handleSliceWithinUpperOrLowerBoundary(src, kind, hi, lo);
    } else {
      // the slice crosses the middle point, so we have to compute upper and lower halves.
      return handleSliceAcrossUpperLowerBoundary(src, kind, hi, lo);
    }
  }

  private ExpressionNode handleSliceWithinUpperOrLowerBoundary(BuiltInCall src, IssMulKind kind,
                                                               int hi, int lo) {
    var a = src.arg(0);
    var b = src.arg(1);
    var targetType = a.type();

    ExpressionNode result;
    if (hi >= targetSize()) {
      // if upper half, we use the mulh operation.
      result = new IssMulhNode(a, b, kind, targetType);

      // adjust the slice by targetSize bit, as we are now handling the upper half only.
      hi -= targetSize();
      lo -= targetSize();
    } else {
      // if lower half, we just use the normal mul built-in.
      result = new BuiltInCall(MUL, src.arguments(), targetType);
    }
    if (lo == 0 && hi == targetSize() - 1) {
      return result;
    }
    return GraphUtils.slice(result, hi, lo);
  }

  private ExpressionNode handleSliceAcrossUpperLowerBoundary(BuiltInCall src, IssMulKind kind,
                                                             int hi, int lo) {
    var a = src.arg(0);
    var b = src.arg(1);

    var targetType = a.type();
    var structType = Type.struct(
        "low", targetType,
        "high", targetType
    );

    var mul2 = new IssMul2Node(a, b, kind, structType);
    // lower and upper half in target type size (not final expected size yet)
    var lowerHalf = new StructGetFieldNode("low", mul2, targetType);
    var upperHalf = new StructGetFieldNode("high", mul2, targetType);

    // lower half sub slice [targetSize - 1 ... lsb]
    var lhMsb = targetSize() - 1;
    var lhLsb = lo;
    // +1 because msb and lsb are inclusive
    var lhSize = lhMsb - lhLsb + 1;

    // upper half sub slice [msb - targetSize ... 0]
    var uhMsb = hi - targetSize();
    var uhLsb = 0;
    var uhSize = uhMsb - uhLsb + 1;

    var finalType = Type.bits(uhSize + lhSize);

    var lhSlice = GraphUtils.zeroExtend(GraphUtils.slice(lowerHalf, lhMsb, lhLsb), finalType);
    var uhSlice = GraphUtils.zeroExtend(GraphUtils.slice(upperHalf, uhMsb, uhLsb), finalType);

    // now we shift the upper half to the correct position uhSlice << lhSize.
    var shiftAmount = new ConstantNode(Constant.Value.of(lhSize, Type.bits(16)));
    var upperHalfShifted = LSL.call(uhSlice, shiftAmount);

    // now we merge both halves into a single value
    return GraphUtils.or(upperHalfShifted, lhSlice);
  }

  private void checkMullSlice(BuiltInCall src, int hi, int lo) {
    var mullSize = src.type().asDataType().bitWidth();
    var sliceSize = hi - lo + 1;
    src.ensure(
        mullSize <= targetSize() * 2,
        "Long multiplication result size (%s) cannot be larger than twice the target size (2 * %s)",
        mullSize, targetSize()
    );
    src.ensure(
        sliceSize <= targetSize(),
        "Long multiplication result size (%s) must be sliced to less than the target size (%s)",
        sliceSize, targetSize()
    );
  }

  private static ExpressionNode boolToWord(ExpressionNode bit, DataType chunkType) {
    return GraphUtils.select(
        bit,
        Constant.Value.one(chunkType).toNode(),
        Constant.Value.zero(chunkType).toNode()
    );
  }
}
