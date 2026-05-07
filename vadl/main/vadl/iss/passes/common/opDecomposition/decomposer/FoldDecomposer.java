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

package vadl.iss.passes.common.opDecomposition.decomposer;

import static vadl.utils.GraphUtils.getSingleNode;

import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.viam.Constant;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FoldNode;

/**
 * Decomposes {@link FoldNode} expressions by materializing only the requested result slice.
 */
public interface FoldDecomposer extends IDecomposer {

  /**
   * Decomposes a requested fold result slice.
   */
  default ExpressionNode decomposeFoldSlice(FoldNode fold, int reqHi, int reqLo) {
    fold.ensure(reqHi >= reqLo, "Expected reqHi >= reqLo, got [%d:%d]", reqHi, reqLo);
    int foldWidth = fold.type().bitWidth();
    fold.ensure(reqHi < foldWidth,
        "Requested slice [%d:%d] exceeds fold width of %d bits", reqHi, reqLo, foldWidth);

    var combiner = combinerBuiltIn(fold);
    if (combiner == BuiltInTable.ADD) {
      return decomposeAddFoldSlice(fold, reqHi, reqLo);
    }
    if (combiner == BuiltInTable.AND
        || combiner == BuiltInTable.OR
        || combiner == BuiltInTable.XOR) {
      return decomposeBitwiseFoldSlice(fold, reqHi, reqLo, combiner);
    }

    fold.ensure(false,
        "Unsupported fold combiner in ISS decomposition: %s (MUL not implemented yet)",
        combiner);
    throw new IllegalStateException("Unreachable");
  }

  private static BuiltInTable.BuiltIn combinerBuiltIn(FoldNode fold) {
    var combinerReturn = getSingleNode(fold.combiner().behavior(), ReturnNode.class);
    fold.ensure(combinerReturn.value() instanceof BuiltInCall,
        "Expected fold combiner to return a BuiltInCall.");
    return ((BuiltInCall) combinerReturn.value()).builtIn();
  }

  private ExpressionNode decomposeBitwiseFoldSlice(FoldNode fold,
                                                   int reqHi,
                                                   int reqLo,
                                                   BuiltInTable.BuiltIn combiner) {
    int reqWidth = reqHi - reqLo + 1;
    var reqType = Type.bits(reqWidth).asDataType();
    var idx = fold.idx();
    var from = idx.fromIdx();
    var to = idx.toIdx();
    var step = from <= to ? 1 : -1;

    ExpressionNode acc;
    if (combiner == BuiltInTable.AND) {
      acc = Constant.Value.zero(reqType).not().toNode();
    } else if (combiner == BuiltInTable.OR || combiner == BuiltInTable.XOR) {
      acc = Constant.Value.zero(reqType).toNode();
    } else {
      throw new IllegalStateException("Unexpected bitwise fold combiner: " + combiner);
    }

    for (int idxValue = from; ; idxValue += step) {
      var bodyAtIdx = ForallSubstitution.copyWithIndexSubstitution(fold.body(), idx, idxValue);
      var part = request(bodyAtIdx, reqHi, reqLo);
      acc = combiner.call(acc, part);
      if (idxValue == to) {
        break;
      }
    }
    return acc;
  }

  private ExpressionNode decomposeAddFoldSlice(FoldNode fold, int reqHi, int reqLo) {
    int reqWidth = reqHi - reqLo + 1;
    fold.ensure(reqWidth <= targetSize(),
        "Fold decomposition request width %d exceeds target size %d", reqWidth, targetSize());

    int foldWidth = fold.type().bitWidth();
    int bodyWidth = fold.body().type().asDataType().bitWidth();
    fold.ensure(bodyWidth == foldWidth,
        "Fold body width mismatch: fold=%d body=%d", foldWidth, bodyWidth);

    int blockSize = targetSize();
    int lastBlock = reqHi / blockSize;
    var mathType = Type.bits(blockSize).asDataType();
    var idx = fold.idx();
    var from = idx.fromIdx();
    var to = idx.toIdx();
    var step = from <= to ? 1 : -1;

    var blockAcc = new ExpressionNode[lastBlock + 1];
    var blockLo = new int[lastBlock + 1];
    var blockHi = new int[lastBlock + 1];
    var blockWidth = new int[lastBlock + 1];
    for (int block = 0; block <= lastBlock; block++) {
      int lo = block * blockSize;
      int hi = Math.min(lo + blockSize - 1, foldWidth - 1);
      blockLo[block] = lo;
      blockHi[block] = hi;
      blockWidth[block] = hi - lo + 1;
      blockAcc[block] = Constant.Value.zero(mathType).toNode();
    }

    for (int idxValue = from; ; idxValue += step) {
      var bodyAtIdx = ForallSubstitution.copyWithIndexSubstitution(fold.body(), idx, idxValue);
      ExpressionNode carry = Constant.Value.zero(Type.bool()).toNode();
      for (int block = 0; block <= lastBlock; block++) {
        var chunkSlice = request(bodyAtIdx, blockHi[block], blockLo[block]);
        var chunkWord = GraphUtils.zeroExtend(chunkSlice, mathType);

        var tmp = BuiltInTable.ADD.call(blockAcc[block], chunkWord);
        var carryWord = boolToWord(carry, mathType);
        var sum = BuiltInTable.ADD.call(tmp, carryWord);

        var c1 = BuiltInTable.ULTH.call(tmp, blockAcc[block]);
        var c2 = BuiltInTable.ULTH.call(sum, tmp);
        carry = GraphUtils.or(c1, c2);

        if (blockWidth[block] < blockSize) {
          var truncated = GraphUtils.slice(sum, blockWidth[block] - 1, 0);
          blockAcc[block] = GraphUtils.zeroExtend(truncated, mathType);
        } else {
          blockAcc[block] = sum;
        }
      }
      if (idxValue == to) {
        break;
      }
    }

    ExpressionNode result = null;
    for (int block = 0; block <= lastBlock; block++) {
      if (blockHi[block] < reqLo || blockLo[block] > reqHi) {
        continue;
      }
      int partLo = Math.max(reqLo, blockLo[block]) - blockLo[block];
      int partHi = Math.min(reqHi, blockHi[block]) - blockLo[block];
      var part = GraphUtils.slice(blockAcc[block], partHi, partLo);
      result = result == null ? part : GraphUtils.concat(part, result);
    }

    fold.ensure(result != null, "Fold ADD decomposition produced no result.");
    return result;
  }

  private static ExpressionNode boolToWord(ExpressionNode bit, DataType wordType) {
    return GraphUtils.select(
        bit,
        Constant.Value.one(wordType).toNode(),
        Constant.Value.zero(wordType).toNode()
    );
  }
}
