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

package vadl.iss.passes.extensions;

import java.util.List;
import javax.annotation.Nullable;
import vadl.iss.passes.common.planning.analysis.VectorRegion;

/**
 * General execution-planning result for one instruction.
 *
 * <p>The public planning boundary is now intentionally small: an instruction either stays on the
 * shared non-helper path or falls back to a whole-instruction helper call. Vector-specific
 * optimization data, such as direct gvec eligibility, is attached as optional region-lowering
 * metadata inside the NORMAL_TCG path.</p>
 */
public record InstrExecPlan(
    ExecutionPath selectedPath,
    List<DirectGvecSupport> directGvecRegions
) {

  public InstrExecPlan {
    directGvecRegions = List.copyOf(directGvecRegions);
  }

  /**
   * Returns whether this instruction stays on the shared non-helper lowering path.
   */
  public boolean usesNormalTcgPath() {
    return selectedPath == ExecutionPath.NORMAL_TCG;
  }

  /**
   * Returns whether this instruction must still execute as a whole-instruction helper call.
   */
  public boolean usesWholeHelperPath() {
    return selectedPath == ExecutionPath.HELPER_CALL;
  }

  /**
   * Returns the viable direct-gvec plans retained for this instruction.
   */
  public List<VectorTensorPlan> directGvecPlans() {
    return directGvecRegions.stream()
        .filter(DirectGvecSupport::isViable)
        .map(DirectGvecSupport::plan)
        .filter(plan -> plan != null)
        .toList();
  }

  /**
   * Returns the first viable direct-gvec plan for this instruction, if any.
   */
  public @Nullable VectorTensorPlan directGvecPlan() {
    return directGvecPlans().stream().findFirst().orElse(null);
  }

  /**
   * Returns whether the planner retained a viable direct-gvec region-lowering plan.
   */
  public boolean hasViableDirectGvecPlan() {
    return directGvecRegions.stream().anyMatch(
        support -> support.isViable() && support.plan() != null
    );
  }

  /**
   * Instruction-level execution path.
   */
  public enum ExecutionPath {
    NORMAL_TCG,
    HELPER_CALL
  }

  /**
   * Outcome of one planning analysis.
   */
  public enum EvaluationStatus {
    VIABLE,
    REJECTED
  }

  /**
   * A diagnostic issue recorded while planning.
   */
  public record PlanningIssue(String code) {

    /**
     * Creates a planning issue from an enum constant.
     */
    public static PlanningIssue of(Enum<?> issueCode) {
      return new PlanningIssue(issueCode.name());
    }
  }

  /**
   * Direct-gvec support result for one instruction.
   *
   * <p>This is region-level optimization metadata, not an instruction execution class.</p>
   */
  public record DirectGvecSupport(
      EvaluationStatus status,
      int estimatedCost,
      List<PlanningIssue> issues,
      VectorRegion region,
      @Nullable VectorTensorPlan plan
  ) {

    public DirectGvecSupport {
      issues = List.copyOf(issues);
    }

    /**
     * Creates a viable direct-gvec support result.
     */
    public static DirectGvecSupport viable(int estimatedCost,
                                           VectorRegion region,
                                           VectorTensorPlan plan) {
      return new DirectGvecSupport(
          EvaluationStatus.VIABLE,
          estimatedCost,
          List.of(),
          region,
          plan
      );
    }

    /**
     * Creates a rejected direct-gvec support result.
     */
    public static DirectGvecSupport rejected(int estimatedCost,
                                             VectorRegion region,
                                             List<PlanningIssue> issues) {
      return new DirectGvecSupport(
          EvaluationStatus.REJECTED,
          estimatedCost,
          issues,
          region,
          null
      );
    }

    /**
     * Returns whether the direct-gvec analysis succeeded.
     */
    public boolean isViable() {
      return status == EvaluationStatus.VIABLE;
    }

    /**
     * Returns whether this analysis recorded the given issue code.
     */
    public boolean hasIssue(String code) {
      return issues.stream().anyMatch(issue -> issue.code().equals(code));
    }
  }
}
