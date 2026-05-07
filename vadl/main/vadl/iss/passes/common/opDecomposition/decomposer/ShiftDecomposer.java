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

import static vadl.error.Diagnostic.ensure;
import static vadl.error.Diagnostic.error;
import static vadl.utils.GraphUtils.bits;
import static vadl.utils.GraphUtils.equ;
import static vadl.utils.GraphUtils.intU;
import static vadl.utils.GraphUtils.neq;
import static vadl.utils.GraphUtils.select;
import static vadl.viam.Constant.Value.zero;

import java.util.ArrayList;
import java.util.List;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.viam.Constant;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * The shift decomposer is responsible for splitting a shift operations into multiple smaller
 * operations that extract a certain data slice of the result,
 * without using > target size operations.
 */
@SuppressWarnings({"LocalVariableName", "ParamterName"})
public interface ShiftDecomposer extends IDecomposer {
  /**
   * De-composes a logical-shift-right call so that **only** the bit-range
   * {@code [hi‥lo]} of the result is materialised.
   *
   * <p>Instead of building the full {@code a >> (b % N)} value (word-width =N),
   * the method:
   * <ol>
   *   <li>retrieves {@code a} and {@code b} from {@code src};</li>
   *   <li>computes the effective distance
   *       {@code eff = b % N}</li>
   *   <li>splits it into whole-chunk and in-chunk parts<br>
   *       {@code sb   = eff / K}   // K = piece size, e.g. 8<br>
   *       {@code sbit = eff % K};</li>
   *   <li>selects just the source pieces that can contribute to bits
   *       {@code hi‥lo} (at most two);</li>
   *   <li>builds those two pieces with 8-bit helpers:<br>
   *       {@code lo = lsr8(src[i+sb], sbit);} <br>
   *       {@code hi = sbit? lsl8(src[i+sb+1], K-sbit) : 0;}<br>
   *       however, this is unrolled and represented as a chain of selects in the dependency graph
   *       </li>
   *   <li>ORs, masks and concatenates so the final node is exactly
   *       {@code hi-lo+1} bits wide.</li>
   * </ol>
   *
   * @param src built-in LSR call {@code LSR(a,b)} (both unsigned)
   * @param hi  most-significant bit of the slice (inclusive, 0 = LSB)
   * @param lo  least-significant bit of the slice
   * @return graph expression that equals {@code (a >> (b % N))[hi:lo]}
   * @throws IllegalArgumentException if {@code hi < lo} or indices exceed word width
   */
  default ExpressionNode lsrDecompose(BuiltInCall src, int hi, int lo) {
    src.ensure(src.builtIn() == BuiltInTable.LSR, "Not a lsr built-in call");
    return shiftDecompose(src, hi, lo, true);
  }

  private ExpressionNode effectiveShiftAmount(ExpressionNode expr, int shiftBits, int ctrlWidth) {
    if (shiftBits < 0) {
      throw new IllegalArgumentException("Expected non-negative shift bit-width, got " + shiftBits);
    }
    if (ctrlWidth <= 0) {
      throw new IllegalArgumentException("Expected positive control width, got " + ctrlWidth);
    }

    if (shiftBits == 0) {
      return Constant.Value.zero(Type.bits(ctrlWidth)).toNode();
    }

    var exprW = expr.type().asDataType().bitWidth();
    ExpressionNode lowBits = exprW >= shiftBits
        ? request(expr, shiftBits - 1, 0)
        : GraphUtils.zeroExtend(expr, Type.bits(shiftBits));

    return lowBits.type().asDataType().bitWidth() == ctrlWidth
        ? lowBits
        : GraphUtils.zeroExtend(lowBits, Type.bits(ctrlWidth));
  }

  /**
   * De-composes a logical-shift-left call so that **only** the bit-range
   * {@code [hi‥lo]} of the result is materialised.
   *
   * <p>Instead of building the full {@code a << (b % N)} value (word-width =N),
   * the method:
   * <ol>
   *   <li>retrieves {@code a} and {@code b} from {@code src};</li>
   *   <li>computes the effective distance
   *       {@code eff = b % N}</li>
   *   <li>splits it into whole-chunk and in-chunk parts<br>
   *       {@code sb   = eff / K}   // K = piece size, e.g. 8<br>
   *       {@code sbit = eff % K};</li>
   *   <li>selects just the source pieces that can contribute to bits
   *       {@code hi‥lo} (at most two);</li>
   *   <li>builds those two pieces with 8-bit helpers:<br>
   *       {@code hi = lsl8(src[i-sb], sbit);} <br>
   *       {@code lo = sbit? lsr8(src[i-sb-1], K-sbit) : 0;}<br>
   *       however, this is unrolled and represented as a chain of selects in the dependency graph
   *       </li>
   *   <li>ORs, masks and concatenates so the final node is exactly
   *       {@code hi-lo+1} bits wide.</li>
   * </ol>
   *
   * @param src built-in LSL call {@code LSL(a,b)} (both unsigned)
   * @param hi  most-significant bit of the slice (inclusive, 0 = LSB)
   * @param lo  least-significant bit of the slice
   * @return graph expression that equals {@code (a << (b % N))[hi:lo]}
   * @throws IllegalArgumentException if {@code hi < lo} or indices exceed word width
   */
  default ExpressionNode lslDecompose(BuiltInCall src, int hi, int lo) {
    src.ensure(src.builtIn() == BuiltInTable.LSL, "Not a lsl built-in call");
    return shiftDecompose(src, hi, lo, false);
  }

  /**
   * Common shift decomposition logic for both LSR and LSL.
   *
   * @param src          built-in shift call
   * @param hi           most-significant bit of the slice
   * @param lo           least-significant bit of the slice
   * @param isRightShift true for LSR, false for LSL
   * @return decomposed shift expression
   */
  private ExpressionNode shiftDecompose(BuiltInCall src, int hi, int lo, boolean isRightShift) {
    var a = src.arg(0);
    var aT = a.type().asDataType();

    // bit width of source type
    final var N = aT.bitWidth();
    ensure(Integer.bitCount(N) == 1, () -> error("Unsupported shift decomposition", src)
        .note("Shift decomposition currently only supports source widths that are powers of two.")
        .note("Got width %s for operation %s.", N, src.builtIn()));

    // shift amount in VADL is b % N, and for power-of-two N this equals low log2(N) bits.
    var shiftBits = Integer.numberOfTrailingZeros(N);
    var ctrlWidth = Math.max(
        shiftBits,
        BitsType.minimalRequiredWidthFor(Math.max(targetSize(), N))
    );
    var b = effectiveShiftAmount(src.arg(1), shiftBits, ctrlWidth);
    var bT = b.type().asDataType();

    // bit width per piece
    final var K = targetSize();
    final var K_NODE = intU(K, bT.bitWidth()).toNode();

    // number of parts that are shifted as a whole (b / K)
    var sp = BuiltInTable.UDIV.call(b, K_NODE);
    var CHUNKS = N / K;
    // maximum possible part that can be shifted as a whole
    var SP_MAX = CHUNKS - 1;
    // number of bits that are shifted within a piece (b % K)
    var sbit = BuiltInTable.UMOD.call(b, K_NODE);

    // a boolean expression if there are any shifts within one piece
    var carry = neq(sbit, zero(sbit.type().asDataType()).toNode());

    // pieces of shift value
    var pieces = getPieces(a, K);

    int firstIdx = lo / K;   // LSB slice
    int lastIdx = hi / K;    // MSB slice
    int offLo = lo % K;      // start bit inside first slice
    int offHi = hi % K;      // end   bit inside last  slice

    // order little endian ... LSB -> MSB
    List<ExpressionNode> outs = new ArrayList<>();
    for (var i = firstIdx; i <= lastIdx; i++) {
      var piece = calculateShiftPiece(i, pieces, sp, sbit, carry, SP_MAX, isRightShift);
      outs.add(piece);
    }

    if (offHi != K - 1) {
      // slice HI piece to correct offHi
      var li = outs.size() - 1;
      var lpiece = outs.get(li);
      outs.set(li, GraphUtils.slice(lpiece, offHi, 0));
    }

    if (offLo != 0) {
      // slice LO piece to correct offLo
      var lpiece = outs.getFirst();
      var pW = lpiece.type().asDataType().bitWidth();
      outs.set(0, GraphUtils.slice(lpiece, pW - 1, offLo));
    }

    // make it big endian (LSB -> MSB to MSB -> LSB)
    outs = outs.reversed();

    return GraphUtils.concat(outs.toArray(ExpressionNode[]::new));
  }

  /**
   * Calculates a single piece of the shift result.
   *
   * @param i            output piece index
   * @param pieces       input pieces
   * @param sp           shift pieces expression
   * @param sbit         shift bits expression
   * @param carry        carry flag expression
   * @param spMax        maximum shift pieces value
   * @param isRightShift true for LSR, false for LSL
   * @return the calculated piece
   */
  private ExpressionNode calculateShiftPiece(int i, List<ExpressionNode> pieces, ExpressionNode sp,
                                             ExpressionNode sbit, ExpressionNode carry, int spMax,
                                             boolean isRightShift) {
    if (isRightShift) {
      var lo = calcShiftPart(i, pieces, sp, spMax, 0, true);
      var hi = calcShiftPart(i, pieces, sp, spMax, 1, true);
      return buildShiftOut(lo, hi, carry, sbit, BuiltInTable.LSR, BuiltInTable.LSL);
    } else {
      var hi = calcShiftPart(i, pieces, sp, spMax, 0, false);
      var lo = calcShiftPart(i, pieces, sp, spMax, 1, false);
      return buildShiftOut(hi, lo, carry, sbit, BuiltInTable.LSL, BuiltInTable.LSR);
    }
  }

  /**
   * Builds output from main and carry parts using specified shift operations.
   *
   * @param mainPart  the main part to shift with sbit
   * @param carryPart the carry part to shift with K-sbit
   * @param carry     carry flag
   * @param sbit      shift bits
   * @param mainOp    shift operation for main part
   * @param carryOp   shift operation for carry part
   * @return combined result
   */
  private ExpressionNode buildShiftOut(ExpressionNode mainPart, ExpressionNode carryPart,
                                       ExpressionNode carry, ExpressionNode sbit,
                                       BuiltInTable.BuiltIn mainOp,
                                       BuiltInTable.BuiltIn carryOp) {
    var pieceSize = mainPart.type().asDataType().bitWidth();
    var pieceSizeNode = GraphUtils.bits(pieceSize, sbit.type().asDataType().bitWidth()).toNode();

    // calculate shift amount for carry part
    var adj = BuiltInTable.SUB.call(pieceSizeNode, sbit);
    // if carry is false, there are no bit shifts
    var select = GraphUtils.select(carry,
        carryOp.call(carryPart, adj),
        Constant.Value.zero(carryPart.type().asDataType()).toNode()
    );

    // shift main part
    var shifted = mainOp.call(mainPart, sbit);
    // merge parts
    return BuiltInTable.OR.call(shifted, select);
  }

  /**
   * Calculates a shift part (main or carry) for a given output piece.
   *
   * @param i            output piece index
   * @param pieces       input pieces
   * @param sp           shift pieces expression
   * @param spMax        maximum shift pieces value
   * @param offset       0 for main part, 1 for carry part
   * @param isRightShift true for LSR, false for LSL
   * @return the calculated part
   */
  private ExpressionNode calcShiftPart(int i, List<ExpressionNode> pieces, ExpressionNode sp,
                                       int spMax, int offset, boolean isRightShift) {
    int pieceW = pieces.getFirst().type().asDataType().bitWidth();
    var zeroExpr = Constant.Value.zero(Type.bits(pieceW)).toNode();

    ExpressionNode result = zeroExpr;

    // For right shift: main from i+sp, carry from i+sp+1
    // For left shift: main from i-sp, carry from i-sp-1
    int sign = isRightShift ? 1 : -1;

    // Early return checks
    if (offset == 1) {
      if (isRightShift && i == spMax) {
        return result; // SP_MAX + 1 is always out of bounds
      }
      if (!isRightShift && i == 0) {
        return result; // 0 - 1 is always out of bounds
      }
    }

    // Determine loop range based on shift direction and offset
    // For LSR: low part j=spMax..0, high part j=spMax..i
    // For LSL: both parts j=spMax..0
    int startJ = spMax;
    int endJ = (offset == 1 && isRightShift) ? i : 0;

    for (int j = startJ; j >= endJ; --j) {
      int srcIdx = i + sign * (j + offset);
      if (srcIdx < 0 || srcIdx >= pieces.size()) {
        continue;
      }

      var target = pieces.get(srcIdx);
      var cmpVal = bits(j, sp.type().asDataType().bitWidth()).toNode();
      var cmp = equ(sp, cmpVal);
      result = select(cmp, target, result);
    }

    return result;
  }

  private List<ExpressionNode> getPieces(ExpressionNode src, int pieceWidth) {
    var srcW = src.type().asDataType().bitWidth();
    // TODO: Support any width
    ensure(srcW % pieceWidth == 0, () -> error("Invalid shift value size", src)
        .note("We currently only support shift values that are a multiple of %s", pieceWidth));

    var pieces = srcW / pieceWidth;
    var piecesList = new ArrayList<ExpressionNode>();
    for (var i = 0; i < pieces; i++) {
      var lo = i * pieceWidth;
      var hi = (i + 1) * pieceWidth - 1;
      piecesList.add(request(src, hi, lo));
    }
    return piecesList;
  }

}
