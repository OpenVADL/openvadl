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

package vadl.lcb.passes.isaMatching.database;

import static vadl.viam.ViamError.ensure;
import static vadl.viam.ViamError.ensurePresent;

import java.util.List;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;

/**
 * This is a container result structure for {@link Database}.
 */
public record QueryResult(Query executedQuery,
                          List<Instruction> machineInstructions,
                          List<PseudoInstruction> pseudoInstructions) {
  /**
   * Get the first machine instruction from the result.
   */
  public Instruction firstMachineInstruction() {
    ensure(pseudoInstructions.isEmpty(),
        "Cannot get first machine instruction when there are pseudo instructions");
    return ensurePresent(machineInstructions.stream().findFirst(),
        "There has to be at least one machine instruction");
  }

  /**
   * Get the first pseudo instruction.
   */
  public PseudoInstruction firstPseudoInstruction() {
    ensure(machineInstructions.isEmpty(),
        "Cannot get first pseudo instruction when there are machine instructions");
    return ensurePresent(pseudoInstructions.stream().findFirst(),
        "There has to be at least one pseudo instruction");
  }
}
