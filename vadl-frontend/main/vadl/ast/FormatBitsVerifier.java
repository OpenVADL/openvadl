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

package vadl.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import javax.annotation.Nullable;
import vadl.types.BitsType;

/**
 * A helper class to determine if a given format fills all the bits it needs to.
 */
class FormatBitsVerifier {
  private final int bitWidth;
  private List<BitState> bitsUsages;
  private int summedTypeWidth = 0;

  FormatBitsVerifier(int bitWidth) {
    this.bitWidth = bitWidth;
    bitsUsages = new ArrayList<>(Collections.nCopies(bitWidth, BitState.UNUSED));
  }

  void addType(BitsType type) {
    summedTypeWidth += type.bitWidth();
  }

  void addRange(int from, int to) {
    for (int i = to; i <= from; i++) {
      bitsUsages.set(i, bitsUsages.get(i).incrementUsage());
    }
  }

  boolean hasViolations() {
    return getViolationsMessage() != null;
  }

  @Nullable
  String getViolationsMessage() {
    // FIXME: For now this class is a bit of a hack because the propper solution is a lot more
    //  involved with finding the correct fields.
    if (summedTypeWidth != 0 && !bitsUsages.stream().allMatch(u -> u == BitState.UNUSED)) {
      return "For now mixed ranges and type fields aren't yet supported";
    }

    if (summedTypeWidth != 0) {
      if (summedTypeWidth != bitWidth) {
        return "Declared as bitwidth of size %d but actually size is %d".formatted(bitWidth,
            summedTypeWidth);
      }
      return null;
    }

    var unUsedBits = new ArrayList<Integer>();
    var doubleUsedBits = new ArrayList<Integer>();
    for (var i = 0; i < bitWidth; i++) {
      if (bitsUsages.get(i) == BitState.UNUSED) {
        unUsedBits.add(i);
      }

      if (bitsUsages.get(i) == BitState.DOUBLE_USED) {
        doubleUsedBits.add(i);
      }
    }

    var message = "";
    if (!unUsedBits.isEmpty()) {
      message += "The following bits are unused: " + prettyRangesString(unUsedBits);
    }
    if (!doubleUsedBits.isEmpty()) {
      if (!message.isEmpty()) {
        message += "\n";
      }
      message +=
          "The following bits are used multiple times: " + prettyRangesString(doubleUsedBits);
    }

    return message.isEmpty() ? null : message;
  }

  /// Creates a pretty string from a list like `[1, 2, 3, 4, 70, 75, 82, 83, 84, 84]` the string
  /// `1-4, 70, 75, 82-24`.
  private static String prettyRangesString(List<Integer> bits) {
    StringJoiner joiner = new StringJoiner(", ");
    for (var i = 0; i < bits.size(); i++) {
      int current = bits.get(i);
      int end = current;

      while (i + 1 < bits.size() && end + 1 == bits.get(i + 1)) {
        i++;
        end = bits.get(i);
      }

      if (current == end) {
        joiner.add(Integer.toString(current));
      } else {
        joiner.add(current + "-" + end);
      }
    }
    return joiner.toString();
  }

  private enum BitState {
    UNUSED,
    USED,
    DOUBLE_USED;

    BitState incrementUsage() {
      return switch (this) {
        case UNUSED -> USED;
        case USED, DOUBLE_USED -> DOUBLE_USED;
      };
    }
  }

}
