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

package vadl.iss.passes.common.planning.analysis.steps;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.EffectFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.LoopFacts;
import vadl.iss.passes.common.planning.analysis.VectorRegion;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.viam.Instruction;
import vadl.viam.graph.control.ForallEndNode;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * Detects vector analysis regions and global loop/effect counts for one instruction.
 */
public final class VectorAccessPatternStep {

  /**
   * Returns global loop-shape facts used for conservative instruction-level planning decisions.
   */
  public LoopFacts loopFacts(Instruction instruction) {
    var foralls = instruction.behavior().getNodes(ForallNode.class).toList();
    return new LoopFacts(foralls.size(), discoverRegions(instruction).size());
  }

  /**
   * Returns global side-effect counts used for conservative instruction-level planning decisions.
   */
  public EffectFacts effectFacts(Instruction instruction) {
    return new EffectFacts(instruction.behavior().getNodes(SideEffectNode.class).toList().size());
  }

  /**
   * Discovers the current set of normalized vector regions in one instruction.
   */
  public List<VectorRegion> discoverRegions(Instruction instruction) {
    var regions = new ArrayList<VectorRegion>();
    var foralls = instruction.behavior().getNodes(ForallNode.class).toList();
    for (int idx = 0; idx < foralls.size(); idx++) {
      var forall = foralls.get(idx);
      var region = regionOf(forall, idx);
      if (region != null) {
        regions.add(region);
      }
    }
    return List.copyOf(regions);
  }

  private @Nullable VectorRegion regionOf(ForallNode forall, int ordinal) {
    if (!(forall.mergeNode() instanceof ForallEndNode forallEnd)) {
      return null;
    }
    // Only one lowered vector write per forall body is considered a first-class analysis region.
    if (forallEnd.sideEffects().size() != 1) {
      return null;
    }
    var sideEffect = forallEnd.sideEffects().getFirst();
    if (!(sideEffect instanceof IssWriteRegNode write)) {
      return null;
    }
    return new VectorRegion(
        "forall-write-" + ordinal,
        forall,
        forallEnd,
        write,
        write.value(),
        forall.idx()
    );
  }
}
