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

package vadl.iss.passes.opDecomposition.decomposer;

import vadl.types.BuiltInTable;
import vadl.utils.GraphUtils;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Decomposes arithmetic operations (SUB) that require carry/borrow handling.
 *
 * <p>Unlike bitwise operations, arithmetic operations cannot be simply decomposed
 * slice by slice because of carry/borrow propagation. This decomposer handles
 * this by computing from bit 0 up to the requested MSB, then extracting the slice.
 */
public interface ArithmeticDecomposer extends IDecomposer {

  /**
   * Decomposes a subtraction call to extract only the bit-range [hi:lo] of the result.
   *
   * <p>For {@code a - b}, this method must handle borrow propagation correctly.
   * The strategy is:
   * <ol>
   *   <li>Request bits [hi:0] from operand {@code a}</li>
   *   <li>Request bits [hi:0] from operand {@code b}</li>
   *   <li>Compute {@code a[hi:0] - b[hi:0]} (this handles borrow correctly)</li>
   *   <li>Extract slice [hi:lo] from the result</li>
   * </ol>
   *
   * @param src built-in SUB call {@code SUB(a,b)}
   * @param hi  most-significant bit of the slice (inclusive, 0 = LSB)
   * @param lo  least-significant bit of the slice
   * @return graph expression that equals {@code (a - b)[hi:lo]}
   */
  default ExpressionNode subDecompose(BuiltInCall src, int hi, int lo) {
    src.ensure(src.builtIn() == BuiltInTable.SUB, "Not a SUB built-in call");

    var a = src.arg(0);
    var b = src.arg(1);

    // To handle borrow correctly, we must compute from bit 0 up to hi
    // Then extract the slice [hi:lo]
    var aSlice = request(a, hi, 0);
    var bSlice = request(b, hi, 0);

    // Perform subtraction on the slices
    // Result has bits [hi:0], which correspond to bits [hi:0] of (a - b)
    var subResult = BuiltInTable.SUB.call(aSlice, bSlice);

    // If lo is 0, we're done - we have exactly [hi:0]
    if (lo == 0) {
      return subResult;
    }

    // Otherwise, extract [hi:lo] from the result
    // The result is (hi+1) bits wide with bit numbering [hi:0]
    // We want to extract [hi:lo], which is [hi:lo] in the result's numbering
    return GraphUtils.slice(subResult, hi, lo);
  }
}
