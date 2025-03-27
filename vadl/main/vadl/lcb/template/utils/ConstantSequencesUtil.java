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
import java.util.List;
import java.util.Map;
import vadl.gcb.passes.GenerateValueRangeImmediatePass;
import vadl.template.Renderable;
import vadl.types.BitsType;
import vadl.viam.CompilerInstruction;
import vadl.viam.Specification;

public class ConstantSequencesUtil {


  record ConstantSequence(CompilerInstruction instruction,
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

  public static List<ConstantSequence> createConstantSequences(Specification specification) {
    var abi = specification.abi().orElseThrow();
    var constantSequences = abi.constantSequences()
        .stream().map(x -> {
          var param = x.getLargestParameter();
          var ty = (BitsType) param.type().asDataType();

          var highest = GenerateValueRangeImmediatePass.highestPossibleValue(ty.bitWidth(), ty);
          var lowest = GenerateValueRangeImmediatePass.lowestPossibleValue(ty.bitWidth(), ty);

          return new ConstantSequence(x,
              Arrays.stream(x.parameters()).anyMatch(y -> y.type().asDataType().isSigned()),
              highest,
              lowest
          );
        })
        .sorted((o1, o2) -> {
          var b1 = o1.instruction.getLargestParameter().type().asDataType().useableBitWidth();
          var b2 = o2.instruction.getLargestParameter().type().asDataType().useableBitWidth();

          return b1 - b2;
        })
        .toList();
    return constantSequences;
  }
}
