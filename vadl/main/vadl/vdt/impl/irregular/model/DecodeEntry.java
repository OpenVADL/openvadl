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

package vadl.vdt.impl.irregular.model;

import java.util.Set;
import vadl.vdt.impl.irregular.IrregularDecodeTreeGenerator;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.Instruction;

/**
 * An decode entry as required by the {@link IrregularDecodeTreeGenerator}.
 */
public class DecodeEntry extends Instruction {

  private final Set<ExclusionCondition> exclusionConditions;

  public DecodeEntry(vadl.viam.Instruction source, int width, BitPattern pattern,
                     Set<ExclusionCondition> exclusionConditions) {
    super(source, width, pattern);
    this.exclusionConditions = exclusionConditions;
  }

  public Set<ExclusionCondition> exclusionConditions() {
    return exclusionConditions;
  }
}
