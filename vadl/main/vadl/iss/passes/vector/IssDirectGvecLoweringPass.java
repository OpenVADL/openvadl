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

package vadl.iss.passes.vector;

import static vadl.iss.passes.TcgPassUtils.instrInfo;

import java.io.IOException;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.AbstractIssPass;
import vadl.iss.passes.extensions.InstrExecPlan.DirectGvecSupport;
import vadl.iss.passes.tcg.lowering.nodes.TcgGvecOpNode;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.GraphUtils;
import vadl.viam.Instruction;
import vadl.viam.Specification;

/**
 * Lowers direct-gvec candidates from their loop/body graph form into backend gvec nodes.
 */
public class IssDirectGvecLoweringPass extends AbstractIssPass {

  public IssDirectGvecLoweringPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("ISS Direct Gvec Lowering");
  }

  @Override
  public @Nullable Object execute(PassResults passResults, Specification viam) throws IOException {
    normalTcgInstrs(viam).forEach(this::lowerInstruction);
    return null;
  }

  private void lowerInstruction(Instruction instruction) {
    var executionPlan = instrInfo(instruction).executionPlan();
    if (executionPlan == null) {
      return;
    }
    var viableRegions = executionPlan.directGvecRegions().stream()
        .filter(DirectGvecSupport::isViable)
        .filter(support -> support.plan() != null)
        .toList();
    if (viableRegions.isEmpty()) {
      return;
    }

    var graph = instruction.behavior();
    for (var support : viableRegions) {
      lowerRegion(support);
    }
    graph.deleteUnusedDependencies();
  }

  private void lowerRegion(DirectGvecSupport support) {
    var region = support.region();
    var plan = support.plan();
    if (plan == null) {
      return;
    }

    var next = region.forallEnd().unlinkNext();
    var gvecNode = region.forall().addBefore(new TcgGvecOpNode(plan));
    gvecNode.setSourceLocationIfNotSet(region.write().location());
    gvecNode.setNext(next);

    GraphUtils.deleteAllBetween(region.forall(), region.forallEnd());
  }
}
