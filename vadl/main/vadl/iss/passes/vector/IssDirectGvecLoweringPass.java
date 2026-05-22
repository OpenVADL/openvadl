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
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.iss.passes.tcgLowering.nodes.TcgGvecOpNode;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.GraphUtils;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.control.ForallEndNode;
import vadl.viam.graph.control.ForallNode;

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
    var plan = instrInfo(instruction).directGvecPlan();
    if (plan == null) {
      return;
    }

    var graph = instruction.behavior();
    var foralls = graph.getNodes(ForallNode.class).toList();
    var forallEnds = graph.getNodes(ForallEndNode.class).toList();
    if (foralls.size() != 1 || forallEnds.size() != 1) {
      throw new ViamGraphError("Expected exactly one forall region for direct-gvec lowering")
          .addContext(instruction)
          .addContext("foralls", foralls)
          .addContext("forallEnds", forallEnds);
    }

    var forall = foralls.getFirst();
    var forallEnd = forallEnds.getFirst();
    var next = forallEnd.unlinkNext();

    var gvecNode = forall.addBefore(new TcgGvecOpNode(plan));
    graph.getNodes(IssWriteRegNode.class).findFirst()
        .ifPresent(w -> gvecNode.setSourceLocationIfNotSet(w.location()));
    gvecNode.setNext(next);

    GraphUtils.deleteAllBetween(forall, forallEnd);
    graph.deleteUnusedDependencies();
  }
}
