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
import javax.annotation.Nullable;
import vadl.iss.passes.extensions.RegInfo;
import vadl.iss.passes.nodes.IssReadRegNode;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.viam.Instruction;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Reusable vector facts extracted from one instruction.
 */
public record VectorInstructionFacts(
    Instruction instruction,
    LoopFacts loop,
    EffectFacts effects,
    List<VectorRegionFacts> regions
) {

  public VectorInstructionFacts {
    regions = List.copyOf(regions);
  }

  /**
   * Loop-shape facts extracted from the instruction body.
   */
  public record LoopFacts(
      int forallCount,
      int recognizedRegionCount
  ) {
  }

  /**
   * Side-effect facts extracted from the instruction body.
   */
  public record EffectFacts(
      int sideEffectCount
  ) {
  }

  /**
   * Strategy-neutral facts extracted for one vector analysis region.
   */
  public record VectorRegionFacts(
      VectorRegion region,
      @Nullable WriteAccessFacts write,
      @Nullable OperationFacts operation,
      List<OperandAccessFacts> operands
  ) {
    public VectorRegionFacts {
      operands = List.copyOf(operands);
    }
  }

  /**
   * Write-side facts derived from the lowered destination access.
   */
  public record WriteAccessFacts(
      IssWriteRegNode write,
      AccessBaseKind baseKind,
      AccessWindowKind windowKind,
      boolean elementShapeMatches,
      boolean conditional,
      StorageFacts storage,
      BindingFacts binding,
      LayoutFacts layout,
      SizeFacts size,
      OverlapFacts overlap
  ) {

    public boolean usesSupportedWindowKind() {
      return windowKind == AccessWindowKind.CHUNK || windowKind == AccessWindowKind.FULL;
    }
  }

  /**
   * Operation-shape facts derived from the vector body value expression.
   */
  public record OperationFacts(
      boolean valueIsBuiltInCall,
      @Nullable BuiltInCall binaryOperation,
      @Nullable ExpressionNode unaryOperand,
      @Nullable OperationKind operationKind
  ) {
  }

  /**
   * Operand-side facts derived from one vector operation argument.
   */
  public record OperandAccessFacts(
      ExpressionNode expression,
      @Nullable IssReadRegNode read,
      ReadView readView,
      OperandShape operandShape,
      AccessBaseKind baseKind,
      AccessWindowKind windowKind,
      boolean elementShapeMatches,
      @Nullable StorageFacts storage,
      boolean widthMatches,
      @Nullable BindingFacts binding
  ) {

    public boolean usesSupportedWindowKind() {
      return windowKind == AccessWindowKind.CHUNK || windowKind == AccessWindowKind.FULL;
    }
  }

  /**
   * Normalized source view used to obtain one vector lane.
   */
  public enum ReadView {
    NONE,
    DIRECT_ELEMENT,
    FULL_REGISTER_LANE_SLICE
  }

  /**
   * Neutralized operand classification used by later strategy evaluators.
   */
  public enum OperandShape {
    VECTOR_REGISTER,
    SCALAR_EXPRESSION,
    IMMEDIATE,
    OTHER
  }

  /**
   * Neutralized base-access classification for lowered register accesses.
   */
  public enum AccessBaseKind {
    BASE,
    ALIAS,
    OTHER
  }

  /**
   * Neutralized window classification for lowered register accesses.
   */
  public enum AccessWindowKind {
    CHUNK,
    FULL,
    OTHER
  }

  /**
   * Neutralized classification of storage properties relevant to later evaluators.
   */
  public record StorageFacts(
      RegInfo.ExecClass execClass,
      boolean envOffsetAddressable,
      boolean byteAddressable,
      int alignmentBytes
  ) {
  }

  /**
   * Neutralized binding facts for vector register selections.
   */
  public record BindingFacts(
      RegisterTensor registerTensor,
      List<ExpressionNode> accessorIndices
  ) {
    public BindingFacts {
      accessorIndices = List.copyOf(accessorIndices);
    }
  }

  /**
   * Neutralized layout facts derived from the selected vector view.
   */
  public record LayoutFacts(
      boolean contiguousElements,
      boolean fullRegisterRange,
      boolean paddingPreserved
  ) {
  }

  /**
   * Neutralized size facts derived from the selected vector view.
   */
  public record SizeFacts(
      int elementBits,
      int laneCount,
      int oprszBytes,
      int maxszBytes
  ) {
  }

  /**
   * Neutralized overlap proof available from the extracted vector shape.
   */
  public enum OverlapFacts {
    NOT_ANALYZED,
    EXACT_OR_DISJOINT_ONLY,
    PARTIAL_POSSIBLE
  }

  /**
   * Neutralized vector-operation classification.
   */
  public enum OperationKind {
    MOV,
    ADD,
    SUB,
    AND,
    OR,
    XOR,
    MUL,
    OTHER
  }
}
