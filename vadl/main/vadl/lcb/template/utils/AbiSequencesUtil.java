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

package vadl.lcb.template.utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import vadl.gcb.passes.GenerateValueRangeImmediatePass;
import vadl.template.Renderable;
import vadl.types.BitsType;
import vadl.viam.CompilerInstruction;
import vadl.viam.Specification;

/**
 * Utility class.
 */
public class AbiSequencesUtil {

  /**
   * Helper record to construct constant sequences.
   */
  public record ConstantSequence(CompilerInstruction instruction,
                                 boolean isSigned,
                                 long highestValue,
                                 long lowestValue) implements Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "instruction", instruction.identifier.simpleName(),
          "isSigned", isSigned,
          "highestValue", highestValue,
          "lowestValue", lowestValue
      );
    }
  }

  /**
   * Create constant sequences from abi.
   */
  public static List<ConstantSequence> createConstantSequences(Specification specification) {
    var abi = specification.abi().orElseThrow();
    return abi.constantSequences()
        .stream().map(AbiSequencesUtil::constantSequence)
        .sorted(sortingFunction())
        .toList();
  }

  /**
   * Create register adjustment sequences from abi.
   */
  public static List<ConstantSequence> createRegisterAdjustment(Specification specification) {
    var abi = specification.abi().orElseThrow();
    return abi.registerAdjustmentSequences()
        .stream().map(AbiSequencesUtil::constantSequence)
        .sorted(sortingFunction())
        .toList();
  }

  /**
   * Sorts the sequences by smallest type first.
   */
  private static @Nonnull Comparator<ConstantSequence> sortingFunction() {
    return (o1, o2) -> {
      var b1 = o1.instruction.getLargestParameter().type().asDataType().useableBitWidth();
      var b2 = o2.instruction.getLargestParameter().type().asDataType().useableBitWidth();

      return b1 - b2;
    };
  }

  private static ConstantSequence constantSequence(CompilerInstruction x) {
    var param = x.getLargestParameter();
    var ty = (BitsType) param.type().asDataType();

    var highest = GenerateValueRangeImmediatePass.highestPossibleValue(ty.bitWidth(), ty);
    var lowest = GenerateValueRangeImmediatePass.lowestPossibleValue(ty.bitWidth(), ty);

    return new ConstantSequence(x,
        Arrays.stream(x.parameters()).anyMatch(y -> y.type().asDataType().isSigned()),
        highest,
        lowest
    );
  }
}
