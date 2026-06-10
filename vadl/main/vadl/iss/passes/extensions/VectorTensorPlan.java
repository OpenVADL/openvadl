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
import vadl.viam.RegisterTensor;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Diagnostic ISS vector translation plan.
 *
 * <p>It stores instruction properties that are important to generate optimized QEMU vector
 * instruction execution.
 */
public record VectorTensorPlan(
    VectorOp op,
    VectorShape shape,
    VectorRegisterBinding destination,
    OverlapPolicy overlapPolicy,
    List<VectorOperand> operands
) {

  /**
   * Creates a direct-gvec candidate plan.
   */
  public static VectorTensorPlan directGvecCandidate(VectorOp op,
                                                     VectorShape shape,
                                                     VectorRegisterBinding destination,
                                                     OverlapPolicy overlapPolicy,
                                                     List<VectorOperand> operands) {
    return new VectorTensorPlan(
        op,
        shape,
        destination,
        overlapPolicy,
        List.copyOf(operands)
    );
  }

  public int elementBits() {
    return shape.elementBits();
  }

  public int laneCount() {
    return shape.laneCount();
  }

  public int opBytes() {
    return shape.oprszBytes();
  }

  /**
   * Vector operation recognized by the first matcher.
   */
  public enum VectorOp {
    NONE,
    ADD,
    SUB,
    AND,
    OR,
    XOR,
    MUL
  }

  /**
   * Proven vector operation shape and size/layout facts for direct-gvec candidates.
   */
  public record VectorShape(
      int elementBits,
      int laneCount,
      int oprszBytes,
      int maxszBytes,
      boolean fullRange,
      boolean contiguousLayout,
      boolean paddingPreserved
  ) {
  }

  /**
   * Register binding that preserves the accessor indices needed for future offset emission.
   */
  public record VectorRegisterBinding(
      RegisterTensor registerTensor,
      List<ExpressionNode> accessorIndices
  ) {
    public VectorRegisterBinding {
      accessorIndices = List.copyOf(accessorIndices);
    }
  }

  /**
   * Operand binding for recognized vector plans.
   */
  public record VectorOperand(OperandKind kind,
                              @Nullable VectorRegisterBinding registerBinding) {

    public static VectorOperand vectorRegister(VectorRegisterBinding binding) {
      return new VectorOperand(OperandKind.VECTOR_REGISTER, binding);
    }
  }

  /**
   * Operand categories expected by later vector planning.
   */
  public enum OperandKind {
    VECTOR_REGISTER,
    SCALAR_REGISTER,
    IMMEDIATE,
    BROADCAST,
    PREDICATE
  }

  /**
   * Register-overlap proof available to a future direct-gvec emitter.
   */
  public enum OverlapPolicy {
    NOT_ANALYZED,
    NO_PARTIAL_OVERLAP
  }
}
