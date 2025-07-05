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

package vadl.viam.passes;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;

/**
 * Iterates over all the instructions and saves the behavior so
 * later passes can access the original version of the graph.
 */
public class SnapshotInstructionBehaviorPass extends Pass {
  public SnapshotInstructionBehaviorPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("SnapshotInstructionBehaviorPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var result = new IdentityHashMap<Instruction, Graph>();

    for (var instruction : viam.isa().map(InstructionSetArchitecture::ownInstructions).orElse(
        Collections.emptyList())) {
      var copy = instruction.behavior().copy();
      result.put(instruction, copy);
    }

    return result;
  }
}
