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

/**
 * General execution-planning result for one instruction.
 *
 * <p>It keeps all evaluated execution strategies, their rejection reasons, and the selected
 * best-performing surviving strategy.</p>
 */
public record InstrExecPlan(
    List<StrategyEvaluation> evaluations,
    StrategyEvaluation selectedEvaluation
) {

  public InstrExecPlan {
    evaluations = List.copyOf(evaluations);
  }

  /**
   * Returns the selected execution strategy.
   */
  public StrategyKind selectedStrategy() {
    return selectedEvaluation.strategy();
  }

  /**
   * Returns the evaluation for the requested strategy, if present.
   */
  public @Nullable StrategyEvaluation evaluation(StrategyKind strategy) {
    return evaluations.stream()
        .filter(evaluation -> evaluation.strategy() == strategy)
        .findFirst()
        .orElse(null);
  }

  /**
   * Backend-independent execution strategy kinds.
   */
  public enum StrategyKind {
    TCG_SCALAR,
    DIRECT_GVEC,
    HELPER_CALL
  }

  /**
   * Outcome of evaluating one candidate execution strategy.
   */
  public enum EvaluationStatus {
    VIABLE,
    REJECTED
  }

  /**
   * A diagnostic issue recorded while evaluating one strategy.
   */
  public record StrategyIssue(String code) {

    /**
     * Creates a strategy issue from an enum constant.
     */
    public static StrategyIssue of(Enum<?> issueCode) {
      return new StrategyIssue(issueCode.name());
    }
  }

  /**
   * One strategy evaluation including viability, issues, and the concrete strategy plan.
   */
  public record StrategyEvaluation(
      StrategyKind strategy,
      EvaluationStatus status,
      int estimatedCost,
      List<StrategyIssue> issues,
      @Nullable StrategyPlan plan
  ) {

    public StrategyEvaluation {
      issues = List.copyOf(issues);
    }

    /**
     * Creates a viable strategy evaluation.
     */
    public static StrategyEvaluation viable(StrategyKind strategy,
                                            int estimatedCost,
                                            StrategyPlan plan) {
      return new StrategyEvaluation(
          strategy,
          EvaluationStatus.VIABLE,
          estimatedCost,
          List.of(),
          plan
      );
    }

    /**
     * Creates a rejected strategy evaluation.
     */
    public static StrategyEvaluation rejected(StrategyKind strategy,
                                              int estimatedCost,
                                              List<StrategyIssue> issues) {
      return new StrategyEvaluation(
          strategy,
          EvaluationStatus.REJECTED,
          estimatedCost,
          issues,
          null
      );
    }

    /**
     * Returns whether this strategy survived evaluation.
     */
    public boolean isViable() {
      return status == EvaluationStatus.VIABLE;
    }

    /**
     * Returns whether this evaluation recorded the given issue code.
     */
    public boolean hasIssue(String code) {
      return issues.stream().anyMatch(issue -> issue.code().equals(code));
    }

    /**
     * Returns the plan cast to the requested type, if it matches.
     */
    public <T extends StrategyPlan> @Nullable T planAs(Class<T> planType) {
      return planType.isInstance(plan) ? planType.cast(plan) : null;
    }
  }

  /**
   * Marker interface for concrete execution-strategy plans.
   */
  public sealed interface StrategyPlan permits HelperCallPlan, TcgScalarPlan, VectorTensorPlan {
  }

  /**
   * Selected when the existing scalar TCG pipeline can lower the instruction directly.
   */
  public record TcgScalarPlan() implements StrategyPlan {
  }

  /**
   * Selected when the instruction must still execute as a whole-instruction helper call.
   */
  public record HelperCallPlan() implements StrategyPlan {
  }
}
