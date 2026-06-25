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

import static vadl.iss.passes.TcgPassUtils.regInfo;

import java.util.List;
import javax.annotation.Nullable;
import vadl.iss.passes.TcgPassUtils;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.AccessBaseKind;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.AccessWindowKind;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.BindingFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.LayoutFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.OperandShape;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.OperationKind;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.ReadView;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.SizeFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.StorageFacts;
import vadl.iss.passes.nodes.IssReadRegNode;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.types.BuiltInTable;
import vadl.viam.ArtificialResource;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.DynSliceNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ForIdxNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * Shared helper logic for vector-analysis steps.
 */
public final class VectorAnalysisSupport {

  private VectorAnalysisSupport() {
  }

  /**
   * Normalizes the expression that produces one lane from a vector register.
   *
   * <p>The original expression remains available to later planners while this match exposes the
   * underlying register read and the proof that the selected value is the current loop lane.</p>
   */
  public static @Nullable LaneReadMatch laneRead(ExpressionNode expression,
                                                 ForIdxNode idx,
                                                 int elementBits) {
    if (expression instanceof IssReadRegNode read) {
      return new LaneReadMatch(
          read,
          ReadView.DIRECT_ELEMENT,
          matchesDirectElementRead(read, idx, elementBits)
      );
    }
    if (!(expression instanceof DynSliceNode slice)
        || !(slice.value() instanceof IssReadRegNode read)
        || read.windowKind() != IssReadRegNode.WindowKind.FULL
        || slice.type().bitWidth() != elementBits
        || !isLoopElementOffset(slice.lsb(), idx, elementBits)
        || !isLaneUpperBound(slice.msb(), slice.lsb(), elementBits)) {
      return null;
    }
    return new LaneReadMatch(read, ReadView.FULL_REGISTER_LANE_SLICE, true);
  }

  /**
   * One proven vector-register source view.
   */
  public record LaneReadMatch(
      IssReadRegNode read,
      ReadView readView,
      boolean elementShapeMatches
  ) {
  }

  /**
   * Classifies an operand for later direct-gvec evaluation without committing to a strategy.
   */
  public static OperandShape operandShape(ExpressionNode expression,
                                          @Nullable IssReadRegNode read,
                                          int elementBits) {
    if (read != null) {
      var storage = storageFacts(read.regTensor());
      if (storage.envOffsetAddressable()) {
        return OperandShape.VECTOR_REGISTER;
      }
    }

    if (isImmediateOperand(expression, elementBits)) {
      return OperandShape.IMMEDIATE;
    }

    if (isScalarOperandExpression(expression, elementBits)) {
      return OperandShape.SCALAR_EXPRESSION;
    }

    return OperandShape.OTHER;
  }

  /**
   * Creates a destination binding and strips the loop index from element-access indices.
   */
  public static BindingFacts bindingFacts(IssWriteRegNode write, ForIdxNode idx) {
    return new BindingFacts(
        write.regTensor(),
        vectorRegisterAccessorIndices(write.indices(), idx)
    );
  }

  /**
   * Creates a source binding and strips the loop index from element-access indices.
   */
  public static BindingFacts bindingFacts(IssReadRegNode read, ForIdxNode idx) {
    return new BindingFacts(
        read.regTensor(),
        vectorRegisterAccessorIndices(read.indices(), idx)
    );
  }

  /**
   * Removes the per-lane loop index from a lowered vector register access when present.
   */
  public static List<ExpressionNode> vectorRegisterAccessorIndices(List<ExpressionNode> indices,
                                                                   ForIdxNode idx) {
    if (!indices.isEmpty() && indices.getLast() == idx) {
      return List.copyOf(indices.subList(0, indices.size() - 1));
    }
    return List.copyOf(indices);
  }

  /**
   * Derives vector shape facts from the destination binding and element/lane counts.
   */
  public static SizeFacts sizeFacts(BindingFacts destination, int elementBits, int laneCount) {
    var oprszBytes = elementBits * laneCount / 8;
    var maxszBits = destination.registerTensor()
        .resultType(destination.accessorIndices().size())
        .bitWidth();
    var maxszBytes = maxszBits / 8;
    return new SizeFacts(
        elementBits,
        laneCount,
        oprszBytes,
        maxszBytes
    );
  }

  /**
   * Checks that the selected vector view is laid out as contiguous element lanes.
   */
  public static LayoutFacts layoutFacts(BindingFacts binding, SizeFacts size) {
    var dimensions = binding.registerTensor().dimensions();
    var baseIndexCount = binding.accessorIndices().size();
    var contiguousElements = dimensions.size() == baseIndexCount + 2
        && dimensions.get(baseIndexCount).size() == size.laneCount()
        && dimensions.getLast().size() == size.elementBits();
    var fullRegisterRange = size.oprszBytes() == size.maxszBytes();
    // Alias expansion and flat raw-vector storage often collapse the lane structure into one
    // contiguous bit container. When the normalized loop covers the whole selected register view,
    // that flat container is still a valid direct-gvec layout even without explicit lane dims.
    if (!contiguousElements && fullRegisterRange) {
      contiguousElements = binding.registerTensor()
          .resultType(binding.accessorIndices().size())
          .bitWidth() == size.elementBits() * size.laneCount();
    }
    return new LayoutFacts(
        contiguousElements,
        fullRegisterRange,
        fullRegisterRange
    );
  }

  /**
   * Returns whether the access kind can be lowered directly against the base CPU-vector storage.
   */
  public static AccessBaseKind gvecAccessBaseKind(IssReadRegNode read) {
    if (read.accessKind() == IssReadRegNode.AccessKind.BASE) {
      return AccessBaseKind.BASE;
    }
    return isSimpleGvecAlias(read.aliasResource()) ? AccessBaseKind.BASE : AccessBaseKind.ALIAS;
  }

  /**
   * Returns whether the access kind can be lowered directly against the base CPU-vector storage.
   */
  public static AccessBaseKind gvecAccessBaseKind(IssWriteRegNode write) {
    if (write.accessKind() == IssWriteRegNode.AccessKind.BASE) {
      return AccessBaseKind.BASE;
    }
    if (write.writeGuardKind() == IssWriteRegNode.WriteGuardKind.ZERO_CONSTRAINT) {
      return AccessBaseKind.ALIAS;
    }
    return isSimpleGvecAlias(write.aliasResource()) ? AccessBaseKind.BASE : AccessBaseKind.ALIAS;
  }

  /**
   * Derives neutral storage facts for a register access.
   */
  public static StorageFacts storageFacts(vadl.viam.RegisterTensor registerTensor) {
    var info = regInfo(registerTensor);
    return new StorageFacts(
        info.execClass(),
        info.isGvecCapable(),
        registerTensor.totalWidth() % 8 == 0,
        info.isCpuVector() ? info.cpuVectorAlignmentBytes() : 0
    );
  }

  /**
   * Normalizes access-base classifications across read and write nodes.
   */
  public static AccessBaseKind accessBaseKind(IssWriteRegNode.AccessKind accessKind) {
    return switch (accessKind) {
      case BASE -> AccessBaseKind.BASE;
      case ALIAS -> AccessBaseKind.ALIAS;
    };
  }

  /**
   * Normalizes access-base classifications across read and write nodes.
   */
  public static AccessBaseKind accessBaseKind(IssReadRegNode.AccessKind accessKind) {
    return switch (accessKind) {
      case BASE -> AccessBaseKind.BASE;
      case ALIAS -> AccessBaseKind.ALIAS;
    };
  }

  /**
   * Normalizes access-window classifications across read and write nodes.
   */
  public static AccessWindowKind accessWindowKind(IssWriteRegNode.WindowKind windowKind) {
    return switch (windowKind) {
      case CHUNK -> AccessWindowKind.CHUNK;
      case FULL -> AccessWindowKind.FULL;
      default -> AccessWindowKind.OTHER;
    };
  }

  /**
   * Normalizes access-window classifications across read and write nodes.
   */
  public static AccessWindowKind accessWindowKind(IssReadRegNode.WindowKind windowKind) {
    return switch (windowKind) {
      case CHUNK -> AccessWindowKind.CHUNK;
      case FULL -> AccessWindowKind.FULL;
      default -> AccessWindowKind.OTHER;
    };
  }

  /**
   * Matches the normalized chunk offset shape {@code idx * elementBits}.
   */
  public static boolean isLoopElementOffset(ExpressionNode expr,
                                            ForIdxNode idx,
                                            int elementBits) {
    if (expr instanceof BuiltInCall call && call.arguments().size() == 2) {
      if (call.builtIn() == BuiltInTable.ADD) {
        return matchesAdditiveLoopOffset(call.arg(0), call.arg(1), idx, elementBits)
            || matchesAdditiveLoopOffset(call.arg(1), call.arg(0), idx, elementBits);
      }
      if (call.builtIn() == BuiltInTable.MUL) {
        return matchesLoopMul(call.arg(0), call.arg(1), idx, elementBits)
            || matchesLoopMul(call.arg(1), call.arg(0), idx, elementBits);
      }
    }
    return false;
  }

  /**
   * Checks whether the final accessor index selects the current loop lane.
   */
  public static boolean isFullyIndexedElementAccess(List<ExpressionNode> indices, ForIdxNode idx) {
    return !indices.isEmpty() && indices.getLast() == idx;
  }

  /**
   * Checks whether an expression is an integer constant with the expected value.
   */
  public static boolean isConstantInt(ExpressionNode expr, int expected) {
    return expr instanceof ConstantNode c && c.constant().asVal().intValue() == expected;
  }

  private static boolean matchesDirectElementRead(IssReadRegNode read,
                                                  ForIdxNode idx,
                                                  int elementBits) {
    if (read.windowKind() == IssReadRegNode.WindowKind.CHUNK) {
      return isLoopElementOffset(read.bitOffset(), idx, elementBits)
          && isConstantInt(read.bitWidth(), elementBits);
    }
    if (read.windowKind() == IssReadRegNode.WindowKind.FULL) {
      return isFullyIndexedElementAccess(read.accessorIndices(), idx);
    }
    return false;
  }

  private static boolean isLaneUpperBound(ExpressionNode msb,
                                          ExpressionNode lsb,
                                          int elementBits) {
    if (!(msb instanceof BuiltInCall call)
        || call.builtIn() != BuiltInTable.ADD
        || call.arguments().size() != 2) {
      return false;
    }
    return matchesLaneUpperBound(call.arg(0), call.arg(1), lsb, elementBits)
        || matchesLaneUpperBound(call.arg(1), call.arg(0), lsb, elementBits);
  }

  private static boolean matchesLaneUpperBound(ExpressionNode maybeLsb,
                                               ExpressionNode maybeWidth,
                                               ExpressionNode lsb,
                                               int elementBits) {
    // Tensor indexing currently represents the dynamic upper boundary as lsb + lane width,
    // whereas register-alias lowering constructs the inclusive form lsb + lane width - 1.
    // The result type fixes the extracted width, so accepting these two producer conventions is
    // still an exact same-lane proof.
    return maybeLsb == lsb
        && (isConstantInt(maybeWidth, elementBits)
        || isConstantInt(maybeWidth, elementBits - 1));
  }

  /**
   * Returns whether an operand is translation-time constant and lane-invariant.
   */
  public static boolean isImmediateOperand(ExpressionNode expression, int elementBits) {
    return expression.type().isDataType()
        && expression.type().asDataType().bitWidth() == elementBits
        && !TcgPassUtils.mustBeScheduled(expression);
  }

  /**
   * Returns whether an operand is a scalar expression that may be materialized before a gvec op.
   */
  public static boolean isScalarOperandExpression(ExpressionNode expression, int elementBits) {
    if (!expression.type().isDataType()
        || expression.type().asDataType().bitWidth() != elementBits
        || !TcgPassUtils.mustBeScheduled(expression)) {
      return false;
    }
    return isScalarOperandCore(expression);
  }

  private static boolean isScalarOperandCore(ExpressionNode expression) {
    if (expression instanceof IssReadRegNode read) {
      return !storageFacts(read.regTensor()).envOffsetAddressable();
    }
    if (expression instanceof ReadMemNode) {
      return false;
    }
    if (expression instanceof BuiltInCall call) {
      return call.arguments().stream()
          .allMatch(VectorAnalysisSupport::isScalarOperandCore);
    }
    return expression.inputs()
        .allMatch(input -> input instanceof ExpressionNode expr && isScalarOperandCore(expr));
  }

  private static boolean matchesLoopMul(Node maybeIdx,
                                        ExpressionNode maybeElementBits,
                                        ForIdxNode idx,
                                        int elementBits) {
    return matchesLoopIndex(maybeIdx, idx) && isConstantInt(maybeElementBits, elementBits);
  }

  private static boolean matchesAdditiveLoopOffset(ExpressionNode maybeZero,
                                                   ExpressionNode remainder,
                                                   ForIdxNode idx,
                                                   int elementBits) {
    return isConstantInt(maybeZero, 0) && isLoopElementOffset(remainder, idx, elementBits);
  }

  private static boolean matchesLoopIndex(Node node, ForIdxNode idx) {
    if (node == idx) {
      return true;
    }
    if (node instanceof ZeroExtendNode zeroExtend) {
      return matchesLoopIndex(zeroExtend.value(), idx);
    }
    if (node instanceof SignExtendNode signExtend) {
      return matchesLoopIndex(signExtend.value(), idx);
    }
    if (node instanceof TruncateNode truncate) {
      return matchesLoopIndex(truncate.value(), idx);
    }
    return false;
  }

  /**
   * Maps supported lowered builtins to the current vector operation enum.
   */
  public static OperationKind operationKindOf(BuiltInTable.BuiltIn builtIn) {
    if (builtIn == BuiltInTable.ADD) {
      return OperationKind.ADD;
    }
    if (builtIn == BuiltInTable.SUB) {
      return OperationKind.SUB;
    }
    if (builtIn == BuiltInTable.AND) {
      return OperationKind.AND;
    }
    if (builtIn == BuiltInTable.OR) {
      return OperationKind.OR;
    }
    if (builtIn == BuiltInTable.XOR) {
      return OperationKind.XOR;
    }
    if (builtIn == BuiltInTable.MUL) {
      return OperationKind.MUL;
    }
    return OperationKind.OTHER;
  }

  private static boolean isSimpleGvecAlias(@Nullable ArtificialResource alias) {
    if (alias == null) {
      return false;
    }
    var semantics = alias.semantics();
    return semantics.aliasSlice() == null && semantics.zeroConstraint() == null;
  }
}
