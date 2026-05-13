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

import java.util.List;
import vadl.iss.passes.common.planning.analysis.VectorCandidate;
import vadl.iss.passes.common.planning.analysis.VectorFactStep;
import vadl.iss.passes.common.planning.analysis.VectorFactsBuilder;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.viam.graph.control.ForallEndNode;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.graph.dependency.SideEffectNode;

/**
 * Detects the current vector instruction shape.
 */
public final class VectorAccessPatternStep implements VectorFactStep {

  @Override
  public void extract(VectorFactsBuilder builder) {
    var graph = builder.instruction().behavior();
    // Later evaluators want loop and side-effect counts even for rejected shapes so they can
    // explain a rejection without re-walking the graph.
    var foralls = graph.getNodes(ForallNode.class).toList();
    builder.setForallCount(foralls.size());
    builder.setSideEffectCount(graph.getNodes(SideEffectNode.class).toList().size());
    analyzeForallPattern(builder, foralls);
  }

  private void analyzeForallPattern(VectorFactsBuilder builder, List<ForallNode> foralls) {
    // No forall means there is no lane-structured vector body for later strategies to reason about.
    if (foralls.isEmpty()) {
      builder.setHasSingleForallRegisterWriteBody(false);
      return;
    }

    // The first vector strategies only know how to reason about one elementwise vector write.
    var forallEnds = builder.instruction().behavior().getNodes(ForallEndNode.class).toList();
    if (forallEnds.size() != 1 || forallEnds.getFirst().sideEffects().size() != 1) {
      builder.setHasSingleForallRegisterWriteBody(false);
      return;
    }

    var sideEffect = forallEnds.getFirst().sideEffects().getFirst();
    // A non-register side effect does not expose the destination binding later strategies need for
    // offset, layout, and overlap reasoning.
    if (!(sideEffect instanceof IssWriteRegNode write)) {
      builder.setHasSingleForallRegisterWriteBody(false);
      return;
    }

    // Candidate extraction stays intentionally minimal: later steps and evaluators decide which
    // write shape, storage kind, and operation forms a strategy can actually accept.
    builder.setHasSingleForallRegisterWriteBody(true);
    var idx = foralls.getFirst().idx();
    builder.setCandidate(new VectorCandidate(
        write,
        write.value(),
        idx
    ));
  }
}
