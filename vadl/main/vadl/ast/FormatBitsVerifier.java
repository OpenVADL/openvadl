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

package vadl.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.types.BitsType;

/**
 * A helper class to determine if a given format fills all the bits it needs to.
 */
class FormatBitsVerifier {
  private final int bitWidth;
  private List<Integer> bitsUssages;
  private List<BitsType> typesToFill = new ArrayList<>();

  FormatBitsVerifier(int bitWidth) {
    this.bitWidth = bitWidth;
    bitsUssages = new ArrayList<>(Collections.nCopies(bitWidth, 0));
  }

  void addType(BitsType type) {
    typesToFill.add(type);
  }

  void addRange(int from, int to) {
    for (int i = to; i <= from; i++) {
      bitsUssages.set(i, bitsUssages.get(i) + 1);
    }
  }

  boolean hasViolations() {
    return getViolationsMessage() != null;
  }

  @Nullable
  String getViolationsMessage() {
    // FIXME: For now this class is a bit of a hack because the propper solution is a lot more
    //  involved with finding the correct fields.
    if (!typesToFill.isEmpty() && !bitsUssages.stream().allMatch(u -> u == 0)) {
      return "For now mixed ranges and type fields aren't yet supported";
    }

    if (!typesToFill.isEmpty()) {
      var actualBitWidth = typesToFill.stream()
          .map(t -> t.bitWidth())
          .reduce((a, b) -> a + b)
          .get();

      if (actualBitWidth != bitWidth) {
        return "Declared as bitwidth of size %d but actually size is %d".formatted(bitWidth,
            actualBitWidth);
      }
      return null;
    }

    var unUsedBits = new ArrayList<Integer>();
    var doubleUsedBits = new ArrayList<Integer>();
    for (var i = 0; i < bitWidth; i++) {
      if (bitsUssages.get(i) == 0) {
        unUsedBits.add(i);
      }

      if (bitsUssages.get(i) > 1) {
        doubleUsedBits.add(i);
      }
    }

    var message = "";
    if (!unUsedBits.isEmpty()) {
      message += "The following bits are unused: "
          + unUsedBits.stream()
          .map(String::valueOf)
          .collect(Collectors.joining(", "));
    }
    if (!doubleUsedBits.isEmpty()) {
      if (!message.isEmpty()) {
        message += "\n";
      }
      message += "The following bits are multiple times: "
          + doubleUsedBits.stream()
          .map(String::valueOf)
          .collect(Collectors.joining(", "));
    }

    return message.isEmpty() ? null : message;
  }

}
