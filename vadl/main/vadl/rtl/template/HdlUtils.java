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

package vadl.rtl.template;

import vadl.types.BoolType;
import vadl.types.DataType;
import vadl.viam.Encoding;
import vadl.viam.Instruction;

public class HdlUtils {

  /**
   * Map VADL type to Chisel type.
   *
   * @param type VADL type
   * @return Chisel type
   */
  public static String type(DataType type) {
    if (type instanceof BoolType) {
      return "Bool()";
    }
    return "Bits(" + type.bitWidth() + ".W)";
  }

  /**
   * Get instruction bit pattern: string of 0, 1, ?.
   *
   * @return bit pattern
   */
  public static String getInstructionBitPattern(Instruction instruction) {
    int width = instruction.format().type().bitWidth();
    var pat = new StringBuilder("?".repeat(width));
    for (Encoding.Field field : instruction.encoding().fieldEncodings()) {
      var binary = field.constant().binary("");
      field.formatField().bitSlice().parts().forEach(part -> {
        for (Integer i : part) {
          var pos = binary.length() - i - 1 + part.lsb();
          if (pos < 0) {
            pat.setCharAt(i, '0');
          } else {
            pat.setCharAt(i, binary.charAt(pos));
          }
        }
      });
    }
    return pat.reverse().toString();
  }

}
