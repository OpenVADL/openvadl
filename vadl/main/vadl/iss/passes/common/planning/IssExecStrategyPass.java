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

package vadl.iss.passes.common.planning;

import static vadl.iss.passes.TcgPassUtils.instrInfo;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Specification;

/**
 * Computes and stores instruction execution plans.
 *
 * <p>The selected execution plan is the backend-independent source of truth for later pass routing
 * and code generation.</p>
 */
public class IssExecStrategyPass extends AbstractIssPass {

  private final InstructionExecutionPlanner executionPlanner;

  public IssExecStrategyPass(IssConfiguration configuration) {
    super(configuration);
    this.executionPlanner = new InstructionExecutionPlanner();
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Exec Strategy Classification");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam) throws IOException {
    if (viam.isa().isEmpty()) {
      return null;
    }

    var isa = viam.isa().get();
    isa.ownInstructions().forEach(
        instr -> instrInfo(instr).setExecutionPlan(executionPlanner.plan(instr))
    );
    return null;
  }
}
