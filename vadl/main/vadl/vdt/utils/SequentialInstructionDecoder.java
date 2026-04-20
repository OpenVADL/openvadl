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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.impl.irregular.model.ExclusionCondition;

/**
 * An in-memory decoder that sequentially runs through the given instructions and returns all
 * matching ones.
 * <br>
 * This is useful in a scenario where the VDT cannot be constructed, possibly due to overlapping
 * encoding definitions.
 */
public class SequentialInstructionDecoder {

  private final List<DecodeEntry> insns;

  public SequentialInstructionDecoder(List<DecodeEntry> insns) {
    this.insns = insns;
  }

  /**
   * Decode the given instruction encoding. If multiple instructions match the given encoding, all
   * of them will be returned.
   *
   * @param insn The encoded instruction
   * @return The list of matching instructions.
   */
  public List<DecodeEntry> decode(BitVector insn) {

    final List<DecodeEntry> result = new ArrayList<>();

    for (DecodeEntry entry : insns) {

      BitPattern p = entry.pattern();

      if (p.width() < insn.width()) {
        p = p.rightPad(insn.width() - p.width());
      } else if (p.width() > insn.width()) {
        p = p.rightTrunc(insn.width());
      }

      assert p.width() == insn.width();

      if (!p.test(insn)) {
        continue;
      }

      if (satisfiesConstraints(entry, insn)) {
        result.add(entry);
      }
    }

    return result;
  }

  private boolean satisfiesConstraints(DecodeEntry e, BitVector insn) {

    if (e.exclusionConditions().isEmpty()) {
      return true;
    }

    for (ExclusionCondition exclusion : e.exclusionConditions()) {

      if (!exclusion.matching().test(insn)) {
        continue;
      }

      if (!satisfiesUnmatching(exclusion.unmatching(), insn)) {
        return false;
      }

    }

    return true;
  }

  private boolean satisfiesUnmatching(Set<BitPattern> unmatching, BitVector insn) {

    if (unmatching.isEmpty()) {
      return false;
    }

    for (BitPattern u : unmatching) {

      if (u.test(insn)) {
        return true;
      }
    }

    return false;
  }

}
