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

import java.math.BigInteger;
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
 * Decomposes bit-counting operations by reducing them to target-sized chunk operations.
 */
public interface CountDecomposer extends IDecomposer {

  /**
   * Count decomposer internal representation of a slice request.
   * Might be removed in the future.
   */
  record CountSliceRequest(
      ExpressionNode value,
      int valueWidth,
      int requestedLo,
      int requestedHi,
      int requestedWidth,
      int relevantHi,
      int workWidth
  ) {
    boolean isTriviallyZero() {
      return requestedLo >= BitsType.minimalRequiredWidthFor(valueWidth);
    }
  }

  /**
   * Count decomposer internal representation of a chunk.
   * Might be removed in the future.
   */
  record CountChunk(ExpressionNode value, int lo, int hi) {
    int leadingOffset(int totalWidth) {
      return totalWidth - hi - 1;
    }
  }

  private static ExpressionNode lowBitsConstant(int value, int width) {
    var mask = BigInteger.ONE.shiftLeft(width).subtract(BigInteger.ONE);
    var truncated = BigInteger.valueOf(value).and(mask);
    return Constant.Value.fromInteger(truncated, Type.bits(width)).toNode();
  }

  private CountSliceRequest prepareCountSlice(BuiltInCall src,
                                              int hi,
                                              int lo,
                                              BuiltInTable.BuiltIn expected) {
    src.ensure(src.builtIn() == expected, "Not a %s built-in call", expected.name());

    var value = src.arg(0);
    var valueWidth = value.type().asDataType().bitWidth();
    var requestedWidth = hi - lo + 1;
    var significantWidth = BitsType.minimalRequiredWidthFor(valueWidth);
    var relevantHi = Math.min(hi, significantWidth - 1);

    return new CountSliceRequest(value, valueWidth, lo, hi, requestedWidth, relevantHi,
        relevantHi + 1);
  }

  private List<CountChunk> requestChunks(CountSliceRequest req) {
    var chunks = new ArrayList<CountChunk>();
    for (int chunkLo = 0; chunkLo < req.valueWidth(); chunkLo += targetSize()) {
      int chunkHi = Math.min(chunkLo + targetSize() - 1, req.valueWidth() - 1);
      chunks.add(new CountChunk(request(req.value(), chunkHi, chunkLo), chunkLo, chunkHi));
    }
    return chunks;
  }

  private ExpressionNode zeroBits(int width) {
    return Constant.Value.zero(Type.bits(width)).toNode();
  }

  private ExpressionNode zeroOf(ExpressionNode expr) {
    return Constant.Value.zero(expr.type().asDataType()).toNode();
  }

  private ExpressionNode addOffset(ExpressionNode base, int offset, int width) {
    return offset == 0 ? base : BuiltInTable.ADD.call(base, lowBitsConstant(offset, width));
  }

  private ExpressionNode finalizeCountSlice(CountSliceRequest req, ExpressionNode result) {
    if (req.requestedLo() == 0 && req.relevantHi() == req.requestedHi()) {
      return result;
    }
    if (req.requestedLo() > req.relevantHi()) {
      return zeroBits(req.requestedWidth());
    }

    var available = GraphUtils.slice(result, req.relevantHi(), req.requestedLo());
    return available.type().asDataType().bitWidth() == req.requestedWidth()
        ? available
        : GraphUtils.zeroExtend(available, Type.bits(req.requestedWidth()));
  }

  private ExpressionNode decomposeDirectionalZeroCount(CountSliceRequest req,
                                                       BuiltInTable.BuiltIn countOp,
                                                       boolean trailing) {
    var chunks = requestChunks(req);
    ExpressionNode result = lowBitsConstant(req.valueWidth(), req.workWidth());

    for (int idx = chunks.size() - 1; idx >= 0; idx--) {
      var chunk = chunks.get(idx);
      var chunkNonZero = GraphUtils.neq(chunk.value(), zeroOf(chunk.value()));
      var chunkCount = request(countOp.call(chunk.value()), req.relevantHi(), 0);
      var offset = trailing ? chunk.lo() : chunk.leadingOffset(req.valueWidth());
      var candidate = addOffset(chunkCount, offset, req.workWidth());
      result = GraphUtils.select(chunkNonZero, candidate, result);
    }

    return result;
  }

  /**
   * Decomposes a population count by summing target-sized chunk population counts.
   */
  default ExpressionNode cobDecompose(BuiltInCall src, int hi, int lo) {
    var req = prepareCountSlice(src, hi, lo, BuiltInTable.COB);
    if (req.isTriviallyZero()) {
      return zeroBits(req.requestedWidth());
    }

    ExpressionNode result = zeroBits(req.workWidth());
    for (var chunk : requestChunks(req)) {
      var chunkCount = request(BuiltInTable.COB.call(chunk.value()), req.relevantHi(), 0);
      result = BuiltInTable.ADD.call(result, chunkCount);
    }

    return finalizeCountSlice(req, result);
  }

  /**
   * Decomposes a leading-zero count by scanning target-sized chunks from most- to
   * least-significant and selecting the first non-zero chunk result.
   */
  default ExpressionNode clzDecompose(BuiltInCall src, int hi, int lo) {
    var req = prepareCountSlice(src, hi, lo, BuiltInTable.CLZ);
    if (req.isTriviallyZero()) {
      return zeroBits(req.requestedWidth());
    }
    return finalizeCountSlice(req,
        decomposeDirectionalZeroCount(req, BuiltInTable.CLZ, false));
  }

  /**
   * Decomposes a trailing-zero count by scanning target-sized chunks from least- to
   * most-significant and selecting the first non-zero chunk result.
   */
  default ExpressionNode ctzDecompose(BuiltInCall src, int hi, int lo) {
    var req = prepareCountSlice(src, hi, lo, BuiltInTable.CTZ);
    if (req.isTriviallyZero()) {
      return zeroBits(req.requestedWidth());
    }
    return finalizeCountSlice(req,
        decomposeDirectionalZeroCount(req, BuiltInTable.CTZ, true));
  }
}
