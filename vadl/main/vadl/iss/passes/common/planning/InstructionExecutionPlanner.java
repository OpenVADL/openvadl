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

import java.util.Comparator;
import java.util.List;
import vadl.iss.passes.extensions.InstrExecPlan;
import vadl.iss.passes.extensions.InstrExecPlan.StrategyEvaluation;
import vadl.iss.passes.extensions.InstrExecPlan.StrategyKind;
import vadl.viam.Instruction;

/**
 * Evaluates all configured execution strategies and selects the best viable one.
 */
final class InstructionExecutionPlanner {

  private final List<StrategyEvaluator> evaluators;

  InstructionExecutionPlanner(List<StrategyEvaluator> evaluators) {
    this.evaluators = List.copyOf(evaluators);
  }

  /**
   * Plans instruction execution by evaluating all strategies and selecting the best viable one.
   */
  InstrExecPlan plan(Instruction instruction) {
    var evaluations = evaluators.stream()
        .map(evaluator -> evaluator.evaluate(instruction))
        .toList();
    var selected = evaluations.stream()
        .filter(StrategyEvaluation::isViable)
        .min(Comparator
            .comparingInt(StrategyEvaluation::estimatedCost)
            .thenComparingInt(evaluation -> strategyPriority(evaluation.strategy())))
        .orElseThrow(() -> new IllegalStateException(
            "Expected at least one viable execution strategy for " + instruction.simpleName()));
    return new InstrExecPlan(evaluations, selected);
  }

  private int strategyPriority(StrategyKind strategy) {
    return switch (strategy) {
      case DIRECT_GVEC -> 0;
      case TCG_SCALAR -> 1;
      case HELPER_CALL -> 2;
    };
  }
}
