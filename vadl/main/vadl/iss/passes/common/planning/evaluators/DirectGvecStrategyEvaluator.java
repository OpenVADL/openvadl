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
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.OperandAccessFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.OperandShape;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.OperationKind;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.OverlapFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.SizeFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.VectorRegionFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.WriteAccessFacts;
import vadl.iss.passes.common.planning.analysis.VectorRegion;
import vadl.iss.passes.common.planning.analysis.VectorStrategyIssueCode;
import vadl.iss.passes.extensions.InstrExecPlan.DirectGvecSupport;
import vadl.iss.passes.extensions.InstrExecPlan.PlanningIssue;
import vadl.iss.passes.extensions.VectorTensorPlan;
import vadl.iss.passes.extensions.VectorTensorPlan.OperandForm;
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
   * Evaluates direct-gvec eligibility for each discovered vector analysis region.
   */
  public DirectGvecEvaluation evaluate(Instruction instruction) {
    var facts = factExtractor.extract(instruction);
    var supports = facts.regions().stream()
        .map(this::evaluateRegion)
        .toList();
    return new DirectGvecEvaluation(facts, supports);
  }

  /**
   * Evaluation result for all direct-gvec region candidates in one instruction.
   */
  public record DirectGvecEvaluation(
      VectorInstructionFacts facts,
      List<DirectGvecSupport> regions
  ) {
    public DirectGvecEvaluation {
      regions = List.copyOf(regions);
    }

    /**
     * Returns whether at least one region is a viable direct-gvec candidate.
     */
    public boolean hasViableRegion() {
      return regions.stream().anyMatch(DirectGvecSupport::isViable);
    }

    /**
     * Returns whether the whole instruction is covered by the currently viable direct-gvec
     * regions under the conservative pre-region-coverage execution model.
     */
    public boolean supportsWholeInstructionLowering() {
      return !regions.isEmpty()
          && facts.loop().forallCount() == regions.size()
          && facts.effects().sideEffectCount() == regions.size()
          && regions.stream().allMatch(DirectGvecSupport::isViable);
    }
  }

  private DirectGvecSupport evaluateRegion(VectorRegionFacts regionFacts) {
    var issues = new ArrayList<PlanningIssue>();
    var region = regionFacts.region();

    recordWriteIssues(regionFacts.write(), issues);

    var vectorOp = evaluateOperation(regionFacts.operation(), issues);
    var operandEvaluation = evaluateOperands(regionFacts.operands(), vectorOp, issues);

    var writeFacts = regionFacts.write();
    if (!issues.isEmpty()
        || writeFacts == null
        || vectorOp == null
        || operandEvaluation == null) {
      return rejected(region, issues);
    }

    // The direct-gvec plan is assembled only after all direct-gvec-specific preconditions hold.
    return DirectGvecSupport.viable(
        DIRECT_GVEC_ESTIMATED_COST,
        region,
        VectorTensorPlan.directGvecCandidate(
            vectorOp,
            operandEvaluation.operandForm(),
            vectorShape(writeFacts.size(), writeFacts.layout()),
            vectorRegisterBinding(writeFacts.binding()),
            overlapPolicy(writeFacts.overlap()),
            operandEvaluation.operands()
        )
    );
  }

  private @Nullable VectorTensorPlan.VectorOp evaluateOperation(
      @Nullable VectorInstructionFacts.OperationFacts operationFacts,
      List<PlanningIssue> issues) {
    // Distinguish "unsupported builtin/value shape" from a recognized neutral operation kind so
    // diagnostics stay useful as more strategies are added.
    if (operationFacts == null || operationFacts.operationKind() == null) {
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

  private @Nullable OperandEvaluation evaluateOperands(
      List<OperandAccessFacts> operandFactsList,
      @Nullable VectorTensorPlan.VectorOp vectorOp,
      List<PlanningIssue> issues
  ) {
    if (vectorOp == VectorTensorPlan.VectorOp.MOV) {
      return evaluateMoveLikeOperands(operandFactsList, issues);
    }

    if (operandFactsList.size() != 2) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.UNSUPPORTED_VALUE_SHAPE));
      return null;
    }

    var lhs = operandFactsList.get(0);
    var rhs = operandFactsList.get(1);
    return switch (rhs.operandShape()) {
      case VECTOR_REGISTER -> evaluateVectorVectorOperands(lhs, rhs, issues);
      case SCALAR_EXPRESSION -> evaluateVectorScalarOperands(lhs, rhs, vectorOp, issues);
      case IMMEDIATE -> evaluateVectorImmediateOperands(lhs, rhs, vectorOp, issues);
      case OTHER -> {
        issues.add(PlanningIssue.of(VectorStrategyIssueCode.OPERAND_NOT_VECTOR_READ));
        yield null;
      }
    };
  }

  private @Nullable OperandEvaluation evaluateMoveLikeOperands(
      List<OperandAccessFacts> operandFactsList,
      List<PlanningIssue> issues
  ) {
    if (operandFactsList.size() != 1) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.UNSUPPORTED_VALUE_SHAPE));
      return null;
    }

    var operandFacts = operandFactsList.getFirst();
    return switch (operandFacts.operandShape()) {
      case VECTOR_REGISTER -> evaluateVectorMoveOperand(operandFacts, issues);
      case SCALAR_EXPRESSION -> evaluateScalarBroadcastOperand(operandFacts, issues);
      case IMMEDIATE -> evaluateImmediateBroadcastOperand(operandFacts, issues);
      case OTHER -> {
        issues.add(PlanningIssue.of(VectorStrategyIssueCode.OPERAND_NOT_VECTOR_READ));
        yield null;
      }
    };
  }

  private @Nullable OperandEvaluation evaluateVectorMoveOperand(
      OperandAccessFacts operandFacts,
      List<PlanningIssue> issues
  ) {
    var operands = new ArrayList<VectorOperand>();
    recordVectorRegisterOperand(operandFacts, issues, operands);
    if (!issues.isEmpty()) {
      return null;
    }
    return new OperandEvaluation(OperandForm.VECTOR_MOVE, List.copyOf(operands));
  }

  private @Nullable OperandEvaluation evaluateScalarBroadcastOperand(
      OperandAccessFacts operandFacts,
      List<PlanningIssue> issues
  ) {
    var operands = new ArrayList<VectorOperand>();
    recordScalarOperand(operandFacts, issues, operands);
    if (!issues.isEmpty()) {
      return null;
    }
    return new OperandEvaluation(OperandForm.SCALAR_BROADCAST, List.copyOf(operands));
  }

  private @Nullable OperandEvaluation evaluateImmediateBroadcastOperand(
      OperandAccessFacts operandFacts,
      List<PlanningIssue> issues
  ) {
    var operands = new ArrayList<VectorOperand>();
    recordImmediateOperand(operandFacts, issues, operands);
    if (!issues.isEmpty()) {
      return null;
    }
    return new OperandEvaluation(OperandForm.IMMEDIATE_BROADCAST, List.copyOf(operands));
  }

  private @Nullable OperandEvaluation evaluateVectorVectorOperands(OperandAccessFacts lhs,
                                                                   OperandAccessFacts rhs,
                                                                   List<PlanningIssue> issues) {
    if (lhs.operandShape() != OperandShape.VECTOR_REGISTER) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.OPERAND_NOT_VECTOR_READ));
      return null;
    }

    var operands = new ArrayList<VectorOperand>();
    recordVectorRegisterOperand(lhs, issues, operands);
    recordVectorRegisterOperand(rhs, issues, operands);
    if (!issues.isEmpty()) {
      return null;
    }
    return new OperandEvaluation(OperandForm.VECTOR_VECTOR, List.copyOf(operands));
  }

  private @Nullable OperandEvaluation evaluateVectorScalarOperands(
      OperandAccessFacts lhs,
      OperandAccessFacts rhs,
      @Nullable VectorTensorPlan.VectorOp vectorOp,
      List<PlanningIssue> issues
  ) {
    if (lhs.operandShape() != OperandShape.VECTOR_REGISTER) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.OPERAND_NOT_VECTOR_READ));
      return null;
    }
    if (!supportsScalarOperand(vectorOp)) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.UNSUPPORTED_OPERATION));
      return null;
    }

    var operands = new ArrayList<VectorOperand>();
    recordVectorRegisterOperand(lhs, issues, operands);
    recordScalarOperand(rhs, issues, operands);
    if (!issues.isEmpty()) {
      return null;
    }
    return new OperandEvaluation(OperandForm.VECTOR_SCALAR, List.copyOf(operands));
  }

  private @Nullable OperandEvaluation evaluateVectorImmediateOperands(
      OperandAccessFacts lhs,
      OperandAccessFacts rhs,
      @Nullable VectorTensorPlan.VectorOp vectorOp,
      List<PlanningIssue> issues
  ) {
    if (lhs.operandShape() != OperandShape.VECTOR_REGISTER) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.OPERAND_NOT_VECTOR_READ));
      return null;
    }
    if (!supportsImmediateOperand(vectorOp)) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.UNSUPPORTED_OPERATION));
      return null;
    }

    var operands = new ArrayList<VectorOperand>();
    recordVectorRegisterOperand(lhs, issues, operands);
    recordImmediateOperand(rhs, issues, operands);
    if (!issues.isEmpty()) {
      return null;
    }
    return new OperandEvaluation(OperandForm.VECTOR_IMMEDIATE, List.copyOf(operands));
  }

  private void recordVectorRegisterOperand(OperandAccessFacts operandFacts,
                                           List<PlanningIssue> issues,
                                           List<VectorOperand> operands) {
    var read = operandFacts.read();
    if (read == null) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.OPERAND_NOT_VECTOR_READ));
      return;
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

  private void recordScalarOperand(OperandAccessFacts operandFacts,
                                   List<PlanningIssue> issues,
                                   List<VectorOperand> operands) {
    if (!operandFacts.widthMatches()) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.READ_WIDTH_MISMATCH));
      return;
    }
    operands.add(VectorOperand.scalar(operandFacts.expression()));
  }

  private void recordImmediateOperand(OperandAccessFacts operandFacts,
                                      List<PlanningIssue> issues,
                                      List<VectorOperand> operands) {
    if (!operandFacts.widthMatches()) {
      issues.add(PlanningIssue.of(VectorStrategyIssueCode.READ_WIDTH_MISMATCH));
      return;
    }
    operands.add(VectorOperand.immediate(operandFacts.expression()));
  }

  private boolean supportsScalarOperand(@Nullable VectorTensorPlan.VectorOp vectorOp) {
    if (vectorOp == null) {
      return false;
    }
    return switch (vectorOp) {
      case ADD, SUB, AND, OR, XOR, MUL -> true;
      case MOV -> false;
    };
  }

  private boolean supportsImmediateOperand(@Nullable VectorTensorPlan.VectorOp vectorOp) {
    if (vectorOp == null) {
      return false;
    }
    return switch (vectorOp) {
      case ADD, AND, OR, XOR, MUL -> true;
      case MOV, SUB -> false;
    };
  }

  private @Nullable VectorTensorPlan.VectorOp vectorOpOf(@Nullable OperationKind operationKind) {
    if (operationKind == null) {
      return null;
    }
    // The neutral operation kind is translated to a direct-gvec-specific opcode only here.
    return switch (operationKind) {
      case MOV -> VectorTensorPlan.VectorOp.MOV;
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

  private DirectGvecSupport rejected(VectorRegion region, List<PlanningIssue> issues) {
    return DirectGvecSupport.rejected(
        DIRECT_GVEC_ESTIMATED_COST,
        region,
        List.copyOf(issues)
    );
  }

  private record OperandEvaluation(
      OperandForm operandForm,
      List<VectorOperand> operands
  ) {
  }
}
