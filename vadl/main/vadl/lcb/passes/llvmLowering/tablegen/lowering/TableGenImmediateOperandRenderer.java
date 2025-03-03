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

package vadl.lcb.passes.llvmLowering.tablegen.lowering;


import vadl.gcb.passes.GenerateValueRangeImmediatePass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;

/**
 * Utility class for mapping into tablegen.
 */
public final class TableGenImmediateOperandRenderer {

  /**
   * Transforms the given {@code operand} into a string which can be used by LLVM's TableGen.
   */
  public static String lower(TableGenImmediateRecord operand) {
    var rawType = operand.rawType();
    int highestPossibleValue =
        GenerateValueRangeImmediatePass.highestPossibleValue(operand.formatFieldBitSize(), rawType);
    int lowestPossibleValue =
        GenerateValueRangeImmediatePass.lowestPossibleValue(operand.formatFieldBitSize(), rawType);
    return String.format("""
            class %s<ValueType ty> : Operand<ty>
            {
              let EncoderMethod = "%s";
              let DecoderMethod = "%s";
            }
                    
            def %s
                : %s<%s>
                , ImmLeaf<%s, [{ return Imm >= %s && Imm <= %s && %s(Imm); }]>;
                
            def %sAsLabel : %s<OtherVT>;
            """, operand.rawName(),
        operand.encoderMethod(),
        operand.decoderMethod(),
        operand.fullname(),
        operand.rawName(),
        operand.llvmType().getLlvmType(),
        operand.llvmType().getLlvmType(),
        lowestPossibleValue,
        highestPossibleValue,
        operand.predicateMethod(),
        operand.rawName(),
        operand.rawName()
    );
  }
}
