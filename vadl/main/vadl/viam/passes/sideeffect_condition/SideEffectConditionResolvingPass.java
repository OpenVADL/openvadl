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

package vadl.viam.passes.sideeffect_condition;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.Instruction;
import vadl.viam.Specification;

/**
 * A pass that finds all instructions in the specification and adds conditions
 * to the {@link vadl.viam.graph.dependency.SideEffectNode}.
 * Those conditions define under what condition the corresponding side effect
 * is executed/takes affect.
 *
 * @see SideEffectConditionResolver
 */
public class SideEffectConditionResolvingPass extends Pass {

  public SideEffectConditionResolvingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("Side-Effect Condition Resolving Pass");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam)
      throws IOException {
    var instructions = ViamUtils
        .findDefinitionsByFilter(viam, d -> d instanceof Instruction);
    for (var instruction : instructions) {
      SideEffectConditionResolver.run(((Instruction) instruction).behavior());
    }

    return null;
  }
}
