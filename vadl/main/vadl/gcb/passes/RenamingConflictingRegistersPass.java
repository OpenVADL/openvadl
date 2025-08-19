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

package vadl.gcb.passes;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
import vadl.viam.graph.ReadsRegisterTensor;
import vadl.viam.graph.WritesRegisterTensor;
import vadl.viam.graph.dependency.FieldRefNode;

/**
 * A pass which renames registers which writes and reads from the same register. This is required
 * since LLVM does not allow that the same register is both an input and output operand. Instead,
 * we rename it and add a constraint that both must be the same later.
 */
public class RenamingConflictingRegistersPass extends Pass {
  public RenamingConflictingRegistersPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("RenamingConflictingRegistersPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    var instructions =
        viam.isa().map(InstructionSetArchitecture::ownInstructions).orElse(Collections.emptyList());

    int counter = 0;
    for (var instruction : instructions) {
      for (var behavior : instruction.behaviors()) {
        var candidates = behavior.getNodes(FieldRefNode.class).toList();

        for (var candidate : candidates) {
          if (candidate.usages().anyMatch(x -> x instanceof ReadsRegisterTensor)
              && candidate.usages().anyMatch(x -> x instanceof WritesRegisterTensor)) {
            var reads = candidate.usages().filter(x -> x instanceof ReadsRegisterTensor).toList();

            for (var read : reads) {
              var node = Objects.requireNonNull(read.graph())
                  .addWithInputs(new RenamedFieldRefNode(candidate.formatField(),
                      candidate.type().asDataType(),
                      counter));
              read.replaceInput(candidate, node);
            }
            counter++;
          }
        }
      }
    }

    return null;
  }
}
