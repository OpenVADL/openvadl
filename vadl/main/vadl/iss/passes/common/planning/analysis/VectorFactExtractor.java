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

package vadl.iss.passes.common.planning.analysis;

import java.util.List;
import vadl.iss.passes.common.planning.analysis.steps.VectorAccessPatternStep;
import vadl.iss.passes.common.planning.analysis.steps.VectorOperandStep;
import vadl.iss.passes.common.planning.analysis.steps.VectorOperationStep;
import vadl.iss.passes.common.planning.analysis.steps.VectorWriteShapeStep;
import vadl.viam.Instruction;

/**
 * Extracts reusable vector facts for one instruction.
 *
 * <p>The extracted facts are strategy-neutral. Strategy evaluators decide which of these facts are
 * required and which issue codes should reject a specific execution strategy.</p>
 */
public final class VectorFactExtractor {

  private final VectorAccessPatternStep accessPatternStep;
  private final List<VectorFactStep> steps;

  /**
   * Creates an extractor with the given ordered fact-extraction steps.
   */
  public VectorFactExtractor(VectorAccessPatternStep accessPatternStep,
                             List<VectorFactStep> steps) {
    this.accessPatternStep = accessPatternStep;
    this.steps = List.copyOf(steps);
  }

  /**
   * Creates the default vector fact extractor used by current strategy evaluators.
   */
  public static VectorFactExtractor defaultExtractor() {
    return new VectorFactExtractor(new VectorAccessPatternStep(), List.of(
        new VectorWriteShapeStep(),
        new VectorOperationStep(),
        new VectorOperandStep()
    ));
  }

  /**
   * Runs all configured fact-extraction steps for one instruction.
   */
  public VectorInstructionFacts extract(Instruction instruction) {
    var regions = accessPatternStep.discoverRegions(instruction).stream()
        .map(region -> {
          var state = new VectorFactsBuilder(instruction, region);
          for (var step : steps) {
            step.extract(state);
          }
          return state.toFacts();
        })
        .toList();
    return new VectorInstructionFacts(
        instruction,
        accessPatternStep.loopFacts(instruction),
        accessPatternStep.effectFacts(instruction),
        regions
    );
  }

  /**
   * Discovers vector regions without running the region-local fact steps.
   */
  public List<VectorRegion> discoverRegions(Instruction instruction) {
    return accessPatternStep.discoverRegions(instruction);
  }

  /**
   * Returns the global loop/effect summary used by conservative instruction-level decisions.
   */
  public VectorInstructionFacts extractGlobalFacts(Instruction instruction) {
    return new VectorInstructionFacts(
        instruction,
        accessPatternStep.loopFacts(instruction),
        accessPatternStep.effectFacts(instruction),
        List.of()
    );
  }
}
