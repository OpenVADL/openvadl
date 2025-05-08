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

package vadl.vdt.utils;

import static vadl.vdt.utils.PBit.Value.ONE;
import static vadl.vdt.utils.PBit.Value.ZERO;

import java.math.BigInteger;
import java.util.List;
import vadl.viam.Constant;
import vadl.viam.Encoding;

/**
 * Utility methods to construct bit patterns used for VDT generation.
 */
public class PatternUtils {

  private PatternUtils() {
    // Static utility class
  }

  /**
   * Returns the bit pattern, where fixed bits in the instruction encoding are set to their
   * respective encoding value. All other bits are set to <i>don't care</i>.
   *
   * @param insn The instruction
   * @return The bit pattern
   */
  public static BitPattern toFixedBitPattern(vadl.viam.Instruction insn) {

    final PBit[] bits = new PBit[insn.format().type().bitWidth()];

    // Initialize all bits to "don't care"
    for (int i = 0; i < bits.length; i++) {
      bits[i] = new PBit(PBit.Value.DONT_CARE);
    }

    // Set fixed bits to their respective encoding value
    for (Encoding.Field encField : insn.encoding().fieldEncodings()) {
      BigInteger fixedValue = encField.constant().integer();

      // Start with the least significant part
      final List<Constant.BitSlice.Part> parts = encField.formatField().bitSlice()
          .parts().toList().reversed();

      int offset = 0;
      for (Constant.BitSlice.Part p : parts) {
        for (int i = p.lsb(); i <= p.msb(); i++) {
          var val = fixedValue.testBit((offset + i) - p.lsb()) ? ONE : ZERO;
          bits[bits.length - (i + 1)] = new PBit(val);
        }
        offset += p.size();
      }
    }

    return new BitPattern(bits);
  }

}
