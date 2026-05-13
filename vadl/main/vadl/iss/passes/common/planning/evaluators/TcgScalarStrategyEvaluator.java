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

import static vadl.iss.passes.TcgPassUtils.regInfo;

import java.util.ArrayList;
import java.util.List;
import vadl.iss.passes.common.planning.StrategyEvaluator;
import vadl.iss.passes.extensions.InstrExecPlan;
import vadl.iss.passes.extensions.InstrExecPlan.StrategyEvaluation;
import vadl.iss.passes.extensions.InstrExecPlan.StrategyIssue;
import vadl.iss.passes.extensions.InstrExecPlan.StrategyKind;
import vadl.iss.passes.extensions.InstrExecPlan.TcgScalarPlan;
import vadl.iss.passes.extensions.RegInfo;
import vadl.iss.passes.nodes.IssReadRegNode;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.viam.Instruction;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.graph.dependency.FoldNode;
import vadl.viam.graph.dependency.TensorNode;

/**
 * Evaluates whether the existing scalar TCG pipeline can lower the instruction directly.
 */
public final class TcgScalarStrategyEvaluator implements StrategyEvaluator {

  private static final int TCG_SCALAR_ESTIMATED_COST = 100;

  @Override
  public StrategyEvaluation evaluate(Instruction instruction) {
    var issues = new ArrayList<StrategyIssue>();
    var behavior = instruction.behavior();

    // Scalar TCG is treated as the existing baseline path. It rejects instructions only when the
    // current scalar lowering pipeline would have to rediscover vector/tensor semantics it does
    // not model directly.
    var hasCpuVectorReads = behavior.getNodes(IssReadRegNode.class)
        .anyMatch(node -> regInfo(node.regTensor()).execClass() == RegInfo.ExecClass.CPU_VECTOR);
    var hasCpuVectorWrites = behavior.getNodes(IssWriteRegNode.class)
        .anyMatch(node -> regInfo(node.regTensor()).execClass() == RegInfo.ExecClass.CPU_VECTOR);
    var hasForall = behavior.getNodes(ForallNode.class).findAny().isPresent();
    var hasTensor = behavior.getNodes(TensorNode.class).findAny().isPresent();
    if (hasCpuVectorReads || hasCpuVectorWrites) {
      issues.add(StrategyIssue.of(IssueCode.USES_CPU_VECTOR_STORAGE));
    }
    if (hasForall) {
      issues.add(StrategyIssue.of(IssueCode.HAS_FORALL));
    }
    if (hasTensor) {
      issues.add(StrategyIssue.of(IssueCode.HAS_TENSOR_EXPR));
    }
    var hasFold = behavior.getNodes(FoldNode.class).findAny().isPresent();
    if (hasFold) {
      issues.add(StrategyIssue.of(IssueCode.HAS_FOLD));
    }

    if (!issues.isEmpty()) {
      return InstrExecPlan.StrategyEvaluation.rejected(
          StrategyKind.TCG_SCALAR,
          TCG_SCALAR_ESTIMATED_COST,
          List.copyOf(issues)
      );
    }

    return InstrExecPlan.StrategyEvaluation.viable(
        StrategyKind.TCG_SCALAR,
        TCG_SCALAR_ESTIMATED_COST,
        new TcgScalarPlan()
    );
  }

  /**
   * Rejection reasons for the current scalar TCG pipeline.
   */
  public enum IssueCode {
    USES_CPU_VECTOR_STORAGE,
    HAS_FORALL,
    HAS_TENSOR_EXPR,
    HAS_FOLD
  }
}
