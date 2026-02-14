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

package vadl.iss.passes.opDecomposition.decomposer;

import vadl.types.Type;
import vadl.utils.GraphUtils;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ForIdxNode;
import vadl.viam.graph.dependency.TensorNode;

/**
 * Decomposes {@link TensorNode} expressions by materializing only the requested slice.
 *
 * <p>The implementation keeps loop structure for fully covered middle elements while specializing
 * boundary elements to minimal requested slices.</p>
 */
public interface TensorDecomposer extends IDecomposer {

  default ExpressionNode decomposeTensorSlice(TensorNode tensor, int reqHi, int reqLo) {
    var idx = tensor.idx();
    var from = idx.fromIdx();
    var to = idx.toIdx();
    var minIdx = Math.min(from, to);
    var maxIdx = Math.max(from, to);
    int elementWidth = tensor.body().type().asDataType().bitWidth();
    int tensorWidth = tensor.type().bitWidth();
    int elementCount = maxIdx - minIdx + 1;

    tensor.ensure(tensorWidth == elementWidth * elementCount,
        "Tensor width mismatch: result width=%d, element width=%d, index range=[%d..%d]",
        tensorWidth, elementWidth, from, to);
    tensor.ensure(reqHi < tensorWidth,
        "Requested slice [%d:%d] exceeds tensor width of %d bits",
        reqHi, reqLo, tensorWidth);

    int firstSlot = Math.max(0, reqLo / elementWidth);
    int lastSlot = Math.min(elementCount - 1, reqHi / elementWidth);
    ExpressionNode result = null;
    for (int slot = firstSlot; slot <= lastSlot; slot++) {
      int slotLsb = slot * elementWidth;
      int loInElement = Math.max(0, reqLo - slotLsb);
      int hiInElement = Math.min(elementWidth - 1, reqHi - slotLsb);
      var part = requestTensorElementSlice(
          tensor, idx, slot + minIdx, hiInElement, loInElement);
      result = result == null ? part : GraphUtils.concat(part, result);
    }
    tensor.ensure(result != null, "Tensor slice decomposition produced no result.");
    return result;
  }

  private ExpressionNode requestTensorElementSlice(TensorNode tensor,
                                                   ForIdxNode idx,
                                                   int idxValue,
                                                   int hiInElement,
                                                   int loInElement) {
    var bodyAtIdx = ForallSubstitution.copyWithIndexSubstitution(tensor.body(), idx, idxValue);
    var elementPart = request(bodyAtIdx, hiInElement, loInElement);
    elementPart.setSourceLocation(tensor.location());
    return elementPart;
  }
}
