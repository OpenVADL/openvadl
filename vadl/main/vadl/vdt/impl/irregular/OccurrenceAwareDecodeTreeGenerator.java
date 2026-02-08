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

package vadl.vdt.impl.irregular;

import org.apache.commons.lang3.NotImplementedException;
import vadl.vdt.impl.irregular.model.DecodeEntries;
import vadl.vdt.model.Node;

/**
 * Decode tree generator largely based on the Qin et al. decode tree construction algorithm using
 * cost minimization on instruction occurrence probabilities.
 *
 * <p>See: <a
 * href="https://dl.acm.org/doi/pdf/10.1145/775832.776027">Automated synthesis of efficient binary
 * decoders for retargetable software toolkits (Wei Qin, Sharad Malik)</a>
 */
public class OccurrenceAwareDecodeTreeGenerator extends IrregularDecodeTreeGenerator {

  private final double memoryPenalty;

  public OccurrenceAwareDecodeTreeGenerator(double memoryPenalty) {
    this.memoryPenalty = memoryPenalty;
  }

  @Override
  protected Node makeNode(DecodeEntries decodeEntries) {
    throw new NotImplementedException("TODO");
  }


}
