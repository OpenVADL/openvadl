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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import vadl.iss.passes.common.planning.analysis.VectorFactExtractor;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.AccessBaseKind;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.BindingFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.LayoutFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.OperationKind;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.OverlapFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.SizeFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.WriteAccessFacts;
import vadl.iss.passes.common.planning.analysis.VectorStrategyIssueCode;
import vadl.iss.passes.extensions.InstrExecPlan.DirectGvecSupport;
import vadl.iss.passes.extensions.InstrExecPlan.PlanningIssue;
import vadl.iss.passes.extensions.VectorTensorPlan;
import vadl.iss.passes.extensions.VectorTensorPlan.OverlapPolicy;
import vadl.iss.passes.extensions.VectorTensorPlan.VectorOperand;
import vadl.iss.passes.extensions.VectorTensorPlan.VectorRegisterBinding;
import vadl.iss.passes.extensions.VectorTensorPlan.VectorShape;
import vadl.viam.Instruction;

/**
 * Evaluates the direct-gvec strategy for vector instructions.
 */
public final class DirectGvecStrategyEvaluator {

  private static final int DIRECT_GVEC_ESTIMATED_COST = 10;

  private final VectorFactExtractor factExtractor = VectorFactExtractor.defaultExtractor();

  /**
   * Evaluates whether the instruction contains a vector region that can be rewritten to backend
   * direct-gvec nodes.
   */
  public DirectGvecSupport evaluate(Instruction instruction) {
    var facts = factExtractor.extract(instruction);
    var issues = new ArrayList<PlanningIssue>();

    // Strategy evaluation is additive: each check only rejects DIRECT_GVEC, not the whole
    // instruction. Other strategies still get the same neutral fact set.
    recordGeneralShapeIssues(facts, issues);

    var candidate = facts.candidate();
    if (candidate == null) {
      return rejected(issues);
    }

    recordWriteIssues(facts.write(), issues);

    var operationFacts = facts.operation();
    var vectorOp = evaluateOperation(operationFacts, issues);

    var operands = recordOperandIssues(facts, issues);
    var writeFacts = facts.write();
    if (!issues.isEmpty() || writeFacts == null || vectorOp == null) {
      return rejected(issues);
    }

    // The direct-gvec plan is assembled only after all direct-gvec-specific preconditions hold.
    return DirectGvecSupport.viable(
        DIRECT_GVEC_ESTIMATED_COST,
        VectorTensorPlan.directGvecCandidate(
            vectorOp,
            vectorShape(writeFacts.size(), writeFacts.layout()),
            vectorRegisterBinding(writeFacts.binding()),
            overlapPolicy(writeFacts.overlap()),
            operands
        )
    );
  }

  private void recordGeneralShapeIssues(VectorInstructionFacts facts,
                                        List<PlanningIssue> issues) {
    // Direct gvec starts from one normalized vector loop. More complex loop structures may still
    // be usable by other vector strategies later.
    if (facts.loop().forallCount() == 0) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.NO_FORALL));
      return;
    }
    if (facts.loop().forallCount() != 1) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.MULTIPLE_FORALLS));
    }
    if (facts.effects().sideEffectCount() != 1) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.EXTRA_SIDE_EFFECTS));
    }
    if (!facts.loop().hasSingleForallRegisterWriteBody()) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.FORALL_WITHOUT_SINGLE_SIDE_EFFECT));
    }
  }

  private @Nullable VectorTensorPlan.VectorOp evaluateOperation(
      @Nullable VectorInstructionFacts.OperationFacts operationFacts,
      List<PlanningIssue> issues) {
    // Distinguish "not a binary builtin shape" from "binary builtin but not one of the currently
    // mapped direct-gvec ops" so diagnostics stay useful as more strategies are added.
    if (operationFacts == null || operationFacts.binaryOperation() == null) {
      if (operationFacts != null && operationFacts.valueIsBuiltInCall()) {
        issues.add(PlanningIssue.of(VectorStrategyIssueCode.UNSUPPORTED_OPERATION));
      } else {
        issues.add(PlanningIssue.of(VectorStrategyIssueCode.UNSUPPORTED_VALUE_SHAPE));
      }
      return null;
    }

    var vectorOp = vectorOpOf(operationFacts.operationKind());
    if (vectorOp == null) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.UNSUPPORTED_OPERATION));
    }
    return vectorOp;
  }

  private void recordWriteIssues(@Nullable WriteAccessFacts writeFacts,
                                 List<PlanningIssue> issues) {
    if (writeFacts == null) {
      return;
    }

    var sizeFacts = writeFacts.size();
    // Direct gvec currently assumes byte-addressable lane sizes with one full vector operation
    // region. Smaller or irregular shapes remain available to other strategies.
    if (sizeFacts.elementBits() <= 0
        || sizeFacts.laneCount() <= 0
        || sizeFacts.elementBits() > 64) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.UNSUPPORTED_ELEMENT_WIDTH));
    }
    if ((sizeFacts.elementBits() * sizeFacts.laneCount()) % 8 != 0) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.NON_BYTE_OPERATION_SIZE));
    }

    // Direct gvec needs a destination access that can be translated into a stable env offset plus
    // a plain contiguous vector region.
    if (writeFacts.baseKind() != AccessBaseKind.BASE || !writeFacts.usesSupportedWindowKind()) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.WRITE_NOT_BASE_CHUNK));
    } else if (!writeFacts.elementShapeMatches()) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.WRITE_NOT_BASE_CHUNK));
    }
    if (writeFacts.conditional()) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.WRITE_HAS_CONDITION));
    }
    if (!writeFacts.storage().envOffsetAddressable()) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.DESTINATION_NOT_GVEC_CAPABLE));
    }
    if (!writeFacts.layout().fullRegisterRange()) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.OP_SIZE_NOT_FULL_RANGE));
    }
    if (!writeFacts.layout().contiguousElements()) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.LAYOUT_NOT_CONTIGUOUS));
    }
  }

  private List<VectorOperand> recordOperandIssues(VectorInstructionFacts facts,
                                                  List<PlanningIssue> issues) {
    var operands = new ArrayList<VectorOperand>();
    for (var operandFacts : facts.operands()) {
      // Direct gvec currently only accepts vector-register operands. Keeping the extracted operand
      // facts explicit lets later strategies reuse the same analysis for scalar or immediate forms.
      var read = operandFacts.read();
      if (read == null) {
        issues.add(PlanningIssue.of(VectorStrategyIssueCode.OPERAND_NOT_VECTOR_READ));
        continue;
      }
      if (operandFacts.baseKind() != AccessBaseKind.BASE
          || !operandFacts.usesSupportedWindowKind()) {
        issues.add(PlanningIssue.of(VectorStrategyIssueCode.READ_NOT_BASE_ELEMENT));
      } else if (!operandFacts.elementShapeMatches()) {
        issues.add(PlanningIssue.of(VectorStrategyIssueCode.READ_OFFSET_MISMATCH));
      }
      var storageFacts = operandFacts.storage();
      if (storageFacts == null || !storageFacts.envOffsetAddressable()) {
        issues.add(PlanningIssue.of(VectorStrategyIssueCode.READ_NOT_GVEC_CAPABLE));
      }
      if (!operandFacts.widthMatches()) {
        issues.add(PlanningIssue.of(VectorStrategyIssueCode.READ_WIDTH_MISMATCH));
      }
      if (operandFacts.binding() != null) {
        operands.add(VectorOperand.vectorRegister(vectorRegisterBinding(operandFacts.binding())));
      }
    }
    return List.copyOf(operands);
  }

  private @Nullable VectorTensorPlan.VectorOp vectorOpOf(@Nullable OperationKind operationKind) {
    if (operationKind == null) {
      return null;
    }
    // The neutral operation kind is translated to a direct-gvec-specific opcode only here.
    return switch (operationKind) {
      case ADD -> VectorTensorPlan.VectorOp.ADD;
      case SUB -> VectorTensorPlan.VectorOp.SUB;
      case AND -> VectorTensorPlan.VectorOp.AND;
      case OR -> VectorTensorPlan.VectorOp.OR;
      case XOR -> VectorTensorPlan.VectorOp.XOR;
      case MUL -> VectorTensorPlan.VectorOp.MUL;
      case OTHER -> null;
    };
  }

  private VectorRegisterBinding vectorRegisterBinding(BindingFacts bindingFacts) {
    return new VectorRegisterBinding(
        bindingFacts.registerTensor(),
        bindingFacts.accessorIndices()
    );
  }

  private VectorShape vectorShape(SizeFacts sizeFacts, LayoutFacts layoutFacts) {
    return new VectorShape(
        sizeFacts.elementBits(),
        sizeFacts.laneCount(),
        sizeFacts.oprszBytes(),
        sizeFacts.maxszBytes(),
        layoutFacts.fullRegisterRange(),
        layoutFacts.contiguousElements(),
        layoutFacts.paddingPreserved()
    );
  }

  private OverlapPolicy overlapPolicy(OverlapFacts overlapFacts) {
    // Strategy-neutral overlap facts are mapped to the narrower direct-gvec overlap policy only
    // when this evaluator commits to a direct-gvec plan.
    return switch (overlapFacts) {
      case NOT_ANALYZED -> OverlapPolicy.NOT_ANALYZED;
      case EXACT_OR_DISJOINT_ONLY -> OverlapPolicy.NO_PARTIAL_OVERLAP;
      case PARTIAL_POSSIBLE -> OverlapPolicy.NOT_ANALYZED;
    };
  }

  private DirectGvecSupport rejected(List<PlanningIssue> issues) {
    return DirectGvecSupport.rejected(DIRECT_GVEC_ESTIMATED_COST, List.copyOf(issues));
  }
}
