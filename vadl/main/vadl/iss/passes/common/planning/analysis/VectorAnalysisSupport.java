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
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.AccessBaseKind;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.AccessWindowKind;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.BindingFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.LayoutFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.OperationKind;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.SizeFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.StorageFacts;
import vadl.iss.passes.nodes.IssReadRegNode;
import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.types.BuiltInTable;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ForIdxNode;

/**
 * Shared helper logic for vector-analysis steps.
 */
public final class VectorAnalysisSupport {

  private VectorAnalysisSupport() {
  }

  /**
   * Returns the read node when the expression is a lowered vector register read.
   */
  public static @Nullable IssReadRegNode vectorRead(ExpressionNode node) {
    return node instanceof IssReadRegNode read ? read : null;
  }

  /**
   * Creates a destination binding and strips the loop index from element-access indices.
   */
  public static BindingFacts bindingFacts(IssWriteRegNode write, ForIdxNode idx) {
    return new BindingFacts(
        write.regTensor(),
        vectorRegisterAccessorIndices(write.accessorIndices(), idx)
    );
  }

  /**
   * Creates a source binding and strips the loop index from element-access indices.
   */
  public static BindingFacts bindingFacts(IssReadRegNode read, ForIdxNode idx) {
    return new BindingFacts(
        read.regTensor(),
        vectorRegisterAccessorIndices(read.accessorIndices(), idx)
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
    return new LayoutFacts(
        contiguousElements,
        fullRegisterRange,
        fullRegisterRange
    );
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
    if (!(expr instanceof BuiltInCall call) || call.builtIn() != BuiltInTable.MUL
        || call.arguments().size() != 2) {
      return false;
    }
    return matchesLoopMul(call.arg(0), call.arg(1), idx, elementBits)
        || matchesLoopMul(call.arg(1), call.arg(0), idx, elementBits);
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

  private static boolean matchesLoopMul(Node maybeIdx,
                                        ExpressionNode maybeElementBits,
                                        ForIdxNode idx,
                                        int elementBits) {
    return maybeIdx == idx && isConstantInt(maybeElementBits, elementBits);
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
}
