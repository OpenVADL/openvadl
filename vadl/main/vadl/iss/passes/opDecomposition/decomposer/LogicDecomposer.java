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
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Decomposes bitwise logical operations (OR, AND, XOR) by requesting slices from operands
 * and combining them with the same operation.
 */
public interface LogicDecomposer extends IDecomposer {

  /**
   * Decomposes a bitwise OR call to extract only the bit-range [hi:lo] of the result.
   *
   * <p>For {@code a | b}, this method:
   * <ol>
   *   <li>requests the slice [hi:lo] from operand {@code a}</li>
   *   <li>requests the slice [hi:lo] from operand {@code b}</li>
   *   <li>ORs the two slices together</li>
   * </ol>
   *
   * @param src built-in OR call {@code OR(a,b)}
   * @param hi most-significant bit of the slice (inclusive, 0 = LSB)
   * @param lo least-significant bit of the slice
   * @return graph expression that equals {@code (a | b)[hi:lo]}
   */
  default ExpressionNode orDecompose(BuiltInCall src, int hi, int lo) {
    src.ensure(src.builtIn() == BuiltInTable.OR, "Not an OR built-in call");

    var a = src.arg(0);
    var b = src.arg(1);

    // Request the same slice from both operands
    var aSlice = request(a, hi, lo);
    var bSlice = request(b, hi, lo);

    // OR the two slices together
    return BuiltInTable.OR.call(aSlice, bSlice);
  }

  /**
   * Decomposes a bitwise AND call to extract only the bit-range [hi:lo] of the result.
   *
   * <p>For {@code a & b}, this method:
   * <ol>
   *   <li>requests the slice [hi:lo] from operand {@code a}</li>
   *   <li>requests the slice [hi:lo] from operand {@code b}</li>
   *   <li>ANDs the two slices together</li>
   * </ol>
   *
   * @param src built-in AND call {@code AND(a,b)}
   * @param hi most-significant bit of the slice (inclusive, 0 = LSB)
   * @param lo least-significant bit of the slice
   * @return graph expression that equals {@code (a & b)[hi:lo]}
   */
  default ExpressionNode andDecompose(BuiltInCall src, int hi, int lo) {
    src.ensure(src.builtIn() == BuiltInTable.AND, "Not an AND built-in call");

    var a = src.arg(0);
    var b = src.arg(1);

    // Request the same slice from both operands
    var aSlice = request(a, hi, lo);
    var bSlice = request(b, hi, lo);

    // AND the two slices together
    return BuiltInTable.AND.call(aSlice, bSlice);
  }

  /**
   * Decomposes a bitwise XOR call to extract only the bit-range [hi:lo] of the result.
   *
   * <p>For {@code a ^ b}, this method:
   * <ol>
   *   <li>requests the slice [hi:lo] from operand {@code a}</li>
   *   <li>requests the slice [hi:lo] from operand {@code b}</li>
   *   <li>XORs the two slices together</li>
   * </ol>
   *
   * @param src built-in XOR call {@code XOR(a,b)}
   * @param hi most-significant bit of the slice (inclusive, 0 = LSB)
   * @param lo least-significant bit of the slice
   * @return graph expression that equals {@code (a ^ b)[hi:lo]}
   */
  default ExpressionNode xorDecompose(BuiltInCall src, int hi, int lo) {
    src.ensure(src.builtIn() == BuiltInTable.XOR, "Not an XOR built-in call");

    var a = src.arg(0);
    var b = src.arg(1);

    // Request the same slice from both operands
    var aSlice = request(a, hi, lo);
    var bSlice = request(b, hi, lo);

    // XOR the two slices together
    return BuiltInTable.XOR.call(aSlice, bSlice);
  }
}
