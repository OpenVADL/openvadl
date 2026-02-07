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

package vadl.vdt.impl.irregular.model;

import java.util.Set;
import vadl.vdt.utils.BitPattern;
import vadl.viam.Instruction;

public class OccurrenceAwareDecodeEntry extends DecodeEntry {

  private final double occurrenceProbability;

  public OccurrenceAwareDecodeEntry(Instruction source, int width,
                                    BitPattern pattern,
                                    Set<ExclusionCondition> exclusionConditions,
                                    double occurrenceProbability) {
    super(source, width, pattern, exclusionConditions);
    this.occurrenceProbability = occurrenceProbability;
  }

  public OccurrenceAwareDecodeEntry(DecodeEntry entry, double occurrenceProbability) {
    this(entry.source(), entry.width(), entry.pattern(), entry.exclusionConditions(),
        occurrenceProbability);
  }

  public double getProbability() {
    return occurrenceProbability;
  }
}
