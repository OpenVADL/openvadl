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

import static vadl.iss.passes.common.planning.analysis.VectorAnalysisSupport.accessWindowKind;
import static vadl.iss.passes.common.planning.analysis.VectorAnalysisSupport.bindingFacts;
import static vadl.iss.passes.common.planning.analysis.VectorAnalysisSupport.gvecAccessBaseKind;
import static vadl.iss.passes.common.planning.analysis.VectorAnalysisSupport.isConstantInt;
import static vadl.iss.passes.common.planning.analysis.VectorAnalysisSupport.isFullyIndexedElementAccess;
import static vadl.iss.passes.common.planning.analysis.VectorAnalysisSupport.isLoopElementOffset;
import static vadl.iss.passes.common.planning.analysis.VectorAnalysisSupport.layoutFacts;
import static vadl.iss.passes.common.planning.analysis.VectorAnalysisSupport.sizeFacts;
import static vadl.iss.passes.common.planning.analysis.VectorAnalysisSupport.storageFacts;

import vadl.iss.passes.common.planning.analysis.VectorFactStep;
import vadl.iss.passes.common.planning.analysis.VectorFactsBuilder;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.OverlapFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.WriteAccessFacts;
import vadl.iss.passes.nodes.IssWriteRegNode;

/**
 * Extracts write-side properties plus destination binding and shape facts.
 */
public final class VectorWriteShapeStep implements VectorFactStep {

  @Override
  public void extract(VectorFactsBuilder builder) {
    var candidate = builder.candidate();
    if (candidate == null) {
      return;
    }

    var elementBits = candidate.write().writeBitWidth();
    var laneCount = candidate.idx().toIdx() - candidate.idx().fromIdx() + 1;
    // Binding extraction strips the lane index so later evaluators see the selected vector
    // register rather than one per-lane access inside the lowered loop body.
    var destination = bindingFacts(candidate.write(), candidate.idx());
    // Size and layout remain strategy-neutral here so later strategies can reuse them with
    // different acceptance rules.
    var size = sizeFacts(destination, elementBits, laneCount);
    var layout = layoutFacts(destination, size);
    var write = candidate.write();
    builder.setWriteFacts(new WriteAccessFacts(
        write,
        gvecAccessBaseKind(write),
        accessWindowKind(write.windowKind()),
        matchesElementShape(write, candidate.idx(), elementBits),
        write.nullableCondition() != null,
        storageFacts(write.regTensor()),
        destination,
        layout,
        size,
        // The extracted shape is a full-lane overwrite where equal register selectors overlap
        // exactly and distinct selectors are disjoint.
        OverlapFacts.EXACT_OR_DISJOINT_ONLY
    ));
  }

  private boolean matchesElementShape(IssWriteRegNode write,
                                      vadl.viam.graph.dependency.ForIdxNode idx,
                                      int elementBits) {
    // Chunk windows are the normalized scalarized form of one lane inside a vector register.
    if (write.windowKind() == IssWriteRegNode.WindowKind.CHUNK) {
      return isLoopElementOffset(write.bitOffset(), idx, elementBits)
          && isConstantInt(write.bitWidth(), elementBits);
    }
    // Full windows are still usable as long as the final accessor index selects the active lane.
    if (write.windowKind() == IssWriteRegNode.WindowKind.FULL) {
      return isFullyIndexedElementAccess(write.accessorIndices(), idx)
          && write.writeBitWidth() == elementBits;
    }
    return false;
  }
}
