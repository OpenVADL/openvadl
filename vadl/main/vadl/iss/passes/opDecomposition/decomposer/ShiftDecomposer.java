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

package vadl.iss.passes.opDecomposition.decomposer;

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

    var a = src.arg(0);
    var aT = a.type().asDataType();

    // bit width of source type
    final var N = aT.bitWidth();

    // shift amount in VADL is b % N
    var b = effectiveShiftAmount(src.arg(1), N);
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
      var piece = calculatePiece(i, pieces, sp, sbit, carry, SP_MAX);
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

  private ExpressionNode calculatePiece(int i, List<ExpressionNode> pieces, ExpressionNode sp,
                                        ExpressionNode sbit, ExpressionNode carry, int spMax) {
    var lo = lsrCalcLow(i, pieces, sp, spMax);
    var hi = lsrCalcHigh(i, pieces, sp, spMax);
    return lsrBuildOut(lo, hi, carry, sbit);
  }

  private ExpressionNode lsrBuildOut(ExpressionNode lo, ExpressionNode hi, ExpressionNode carry,
                                     ExpressionNode sbit) {
    // constants
    var pieceSize = lo.type().asDataType().bitWidth();
    var pieceSizeNode = GraphUtils.bits(pieceSize, sbit.type().asDataType().bitWidth()).toNode();

    // calulate the << shift amount to place the hi bits in place of the result
    var adj = BuiltInTable.SUB.call(pieceSizeNode, sbit);
    // if carry is false, there are no bit shifts (only whole piece shifts (sp), and therefore
    // we don't have to place the higher bits into position
    var select = GraphUtils.select(carry,
        BuiltInTable.LSL.call(hi, adj),
        Constant.Value.zero(hi.type().asDataType()).toNode()
    );

    // we shift lo part of the piece by the shift bits
    var lsr = BuiltInTable.LSR.call(lo, sbit);
    // we merge upper and lower part to one
    return BuiltInTable.OR.call(lsr, select);
  }

  private ExpressionNode lsrCalcLow(int i,
                                    List<ExpressionNode> pieces,
                                    ExpressionNode sp,
                                    int spMax) {
    int pieceW = pieces.getFirst().type().asDataType().bitWidth();
    var zeroExpr = Constant.Value.zero(Type.bits(pieceW)).toNode();
    int last = pieces.size() - 1;          // index of the MSB slice

    ExpressionNode lo = zeroExpr;              // default = 0

    /* build a chain of  selects:  (sp==j ? pieces[i+j] : prev) ------ */
    for (int j = spMax; j >= 0; --j) {        // j = 0 … SP_MAX
      int srcIdx = i + j;                    // slice that would land here
      if (srcIdx > last) {
        continue;           // shifts beyond word ⇒ 0
      }

      var target = pieces.get(srcIdx);       // valid source slice

      // constant `j`
      var cmpVal = bits(j, sp.type().asDataType().bitWidth()).toNode();
      var cmp = equ(sp, cmpVal);
      lo = select(cmp, target, lo);
    }
    return lo;                                 // 8-bit data-flow only
  }

  private ExpressionNode lsrCalcHigh(int i, List<ExpressionNode> pieces, ExpressionNode sp,
                                     int spMax) {
    int pieceW = pieces.getFirst().type().asDataType().bitWidth();
    var zeroExpr = Constant.Value.zero(Type.bits(pieceW)).toNode();
    int last = pieces.size() - 1;          // index of the MSB slice

    ExpressionNode hi = zeroExpr;

    if (i == spMax) {
      // not possible, SP_MAX + 1 is always 0
      return hi;
    }

    for (var j = spMax; j >= i; --j) {
      int srcIdx = i + j + 1;
      if (srcIdx > last) {
        continue;
      }
      var target = pieces.get(srcIdx);
      var cmpVal = bits(j, sp.type().asDataType().bitWidth()).toNode();
      var cmp = equ(sp, cmpVal);
      hi = select(cmp, target, hi);
    }
    return hi;
  }

  private ExpressionNode effectiveShiftAmount(ExpressionNode expr, int srcW) {
    var exprW = expr.type().asDataType().bitWidth();
    var srcMinW = BitsType.minimalRequiredWidthFor(srcW);
    var t = Type.bits(Math.max(srcMinW, exprW));
    expr = GraphUtils.zeroExtend(expr, t);
    return BuiltInTable.UMOD.call(expr, Constant.Value.of(srcW, t).toNode());
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
