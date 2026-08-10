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

package vadl.gcb.passes.assembly;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
import vadl.viam.passes.functionInliner.Inliner;

/**
 * Inlines function calls inside every instruction's and pseudo instruction's
 * {@code assembly} definition.
 */
public class AssemblyFunctionInlinerPass extends Pass {

  public AssemblyFunctionInlinerPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return new PassName("AssemblyFunctionInlinerPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {
    viam.isa().map(InstructionSetArchitecture::ownInstructions).orElse(List.of())
        .forEach(instruction -> Inliner.inlineFuncs(instruction.assembly().function().behavior()));

    viam.isa().map(InstructionSetArchitecture::ownPseudoInstructions).orElse(List.of())
        .forEach(instruction -> Inliner.inlineFuncs(instruction.assembly().function().behavior()));

    return null;
  }
}
