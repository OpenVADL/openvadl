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

package vadl.lcb.passes.llvmLowering;

import javax.annotation.Nullable;
import vadl.gcb.passes.ValueRange;

/**
 * An addressing mode in the shape of LLVM's {@code TargetLowering::AddrMode}:
 * {@code BaseReg + Scale * ScaledReg + BaseOffs}. A base register is always present.
 *
 * @param kind        whether the mode was found on a load or a store.
 * @param accessBytes the number of bytes the memory access reads or writes.
 * @param scale       which values {@code AM.Scale} may take.
 * @param offset      which values {@code AM.BaseOffs} may take. {@code null} when the mode
 *                    has no immediate offset, i.e., {@code AM.BaseOffs} must be zero.
 */
public record AddressingMode(Kind kind, int accessBytes, Scale scale, @Nullable Offset offset) {

  /**
   * The kind of memory access the addressing mode is available for.
   */
  public enum Kind {
    LOAD,
    STORE
  }

  /**
   * Which multipliers LLVM's {@code AM.Scale} may take.
   */
  public sealed interface Scale {
    // no scaled register: AM.Scale == 0
    record None() implements Scale {}

    // rb << c or rb * c: AM.Scale == value
    record Fixed(long value) implements Scale {}

    // rb << imm: AM.Scale == 1 << k for k in shiftAmounts
    record EncodedShift(ValueRange shiftAmounts) implements Scale {}

    // rb * imm: AM.Scale in factors
    record EncodedMul(ValueRange factors) implements Scale {}
  }

  /**
   * Which values LLVM's {@code AM.BaseOffs} may take.
   * For {@code ra + (imm << c)}: min = lo << c, max = hi << c, multipleOf = 1 << c.
   */
  public record Offset(long min, long max, long multipleOf) {}
}
