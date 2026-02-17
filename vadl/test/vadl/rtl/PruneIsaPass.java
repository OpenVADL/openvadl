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

package vadl.rtl;

import java.io.IOException;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;

/**
 * Pass for testing that removes VIAM elements to get a simple test case.
 */
public class PruneIsaPass extends Pass {

  private final Set<String> instructions;

  private final boolean regTensorConstraints;

  /**
   * New prune ISA pass that removes all instructions, but the ones referenced by a set of names.
   * If the set is empty, all instructions are kept.
   *
   * @param config       configuration
   * @param instructions set of instruction names
   */
  public PruneIsaPass(GeneralConfiguration config, Set<String> instructions) {
    super(config);
    this.instructions = instructions;
    this.regTensorConstraints = true;
  }

  /**
   * New prune ISA pass that removes all instructions, but the ones referenced by a set of names.
   * If the set is empty, all instructions are kept. Optionally, remove register tensor constraints.
   *
   * @param config               configuration
   * @param instructions         set of instruction names
   * @param regTensorConstraints keep register tensor constraints, if true
   */
  public PruneIsaPass(GeneralConfiguration config, Set<String> instructions,
                      boolean regTensorConstraints) {
    super(config);
    this.instructions = instructions;
    this.regTensorConstraints = regTensorConstraints;
  }

  @Override
  public PassName getName() {
    return PassName.of("Prune ISA");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    viam.isa().ifPresent(isa -> {
      if (!instructions.isEmpty()) {
        isa.ownInstructions().removeIf(ins -> !instructions.contains(ins.simpleName()));
      }
      if (!regTensorConstraints) {
        for (RegisterTensor regTensor : isa.registerTensors()) {
          regTensor.setConstraints();
        }
      }
    });
    return null;
  }

}
