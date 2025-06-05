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


import java.util.Map;
import org.apache.commons.text.StringSubstitutor;
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
    var highestPossibleValue =
        GenerateValueRangeImmediatePass.highestPossibleValue(operand.formatFieldBitSize(), rawType);
    var lowestPossibleValue =
        GenerateValueRangeImmediatePass.lowestPossibleValue(operand.formatFieldBitSize(), rawType);
    return StringSubstitutor.replace("""
                class [${rawName}]<ValueType ty> : Operand<ty>
                {
                  let EncoderMethod = "[${encoderMethod}]";
                  let DecoderMethod = "[${decoderMethod}]";
                }

                def [${fullName}]
                    : [${rawName}]<[${type}]>
                    , ImmLeaf<[${type}], [{ return Imm >= [${lowestPossibleValue}] && Imm <= [${highestPossibleValue}] && [${predicateMethod}](Imm); }]>;

                def [${rawName}]AsLabel : [${rawName}]<OtherVT>;
            """,
        Map.of("rawName", operand.rawName(),
            "fullName", operand.fullname(),
            "encoderMethod", operand.encoderMethod(),
            "decoderMethod", operand.decoderMethod(),
            "type", operand.llvmType().getLlvmType(),
            "lowestPossibleValue", lowestPossibleValue,
            "highestPossibleValue", highestPossibleValue,
            "predicateMethod", operand.predicateMethod()));
  }
}
