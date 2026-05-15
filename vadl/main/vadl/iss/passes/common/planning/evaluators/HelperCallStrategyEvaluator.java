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

package vadl.iss.passes.common.planning.evaluators;

import vadl.iss.passes.common.planning.StrategyEvaluator;
import vadl.iss.passes.extensions.InstrExecPlan;
import vadl.iss.passes.extensions.InstrExecPlan.HelperCallPlan;
import vadl.iss.passes.extensions.InstrExecPlan.StrategyEvaluation;
import vadl.iss.passes.extensions.InstrExecPlan.StrategyKind;
import vadl.viam.Instruction;

/**
 * Fallback evaluator that always keeps whole-instruction helper execution available.
 */
public final class HelperCallStrategyEvaluator implements StrategyEvaluator {

  private static final int HELPER_CALL_ESTIMATED_COST = 1_000;

  @Override
  public StrategyEvaluation evaluate(Instruction instruction) {
    // Whole-instruction helper execution stays always viable so the planner can reject optimized
    // strategies locally without losing a correct fallback.
    return InstrExecPlan.StrategyEvaluation.viable(
        StrategyKind.HELPER_CALL,
        HELPER_CALL_ESTIMATED_COST,
        new HelperCallPlan()
    );
  }
}
