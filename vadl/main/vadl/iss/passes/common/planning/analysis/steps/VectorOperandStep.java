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

package vadl.iss.passes.common.planning.analysis.steps;

import static vadl.iss.passes.common.planning.analysis.VectorAnalysisSupport.accessBaseKind;
import static vadl.iss.passes.common.planning.analysis.VectorAnalysisSupport.accessWindowKind;
import static vadl.iss.passes.common.planning.analysis.VectorAnalysisSupport.bindingFacts;
import static vadl.iss.passes.common.planning.analysis.VectorAnalysisSupport.isConstantInt;
import static vadl.iss.passes.common.planning.analysis.VectorAnalysisSupport.isFullyIndexedElementAccess;
import static vadl.iss.passes.common.planning.analysis.VectorAnalysisSupport.isLoopElementOffset;
import static vadl.iss.passes.common.planning.analysis.VectorAnalysisSupport.storageFacts;
import static vadl.iss.passes.common.planning.analysis.VectorAnalysisSupport.vectorRead;

import javax.annotation.Nullable;
import vadl.iss.passes.common.planning.analysis.VectorFactStep;
import vadl.iss.passes.common.planning.analysis.VectorFactsBuilder;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.OperandAccessFacts;
import vadl.iss.passes.nodes.IssReadRegNode;

/**
 * Extracts operand-side access and binding facts from the matched vector operation.
 */
public final class VectorOperandStep implements VectorFactStep {

  @Override
  public void extract(VectorFactsBuilder builder) {
    var candidate = builder.candidate();
    var writeFacts = builder.writeFacts();
    var operationFacts = builder.operationFacts();
    var operationCall = operationFacts == null ? null : operationFacts.binaryOperation();
    if (candidate == null || writeFacts == null || operationCall == null) {
      return;
    }

    for (var arg : operationCall.arguments()) {
      // Each operand is recorded independently so different strategies can make different decisions
      // about vector, scalar, immediate, alias, or broadcast forms from the same fact set.
      var read = vectorRead(arg);
      builder.addOperandFact(operandFact(
          arg,
          read,
          candidate.idx(),
          writeFacts.size().elementBits()
      ));
    }
  }

  private OperandAccessFacts operandFact(vadl.viam.graph.dependency.ExpressionNode expression,
                                         @Nullable IssReadRegNode read,
                                         vadl.viam.graph.dependency.ForIdxNode idx,
                                         int elementBits) {
    // Non-register operands are kept as explicit facts rather than dropped so evaluators can
    // distinguish scalar/immediate rejection from "no analysis result".
    if (read == null) {
      return new OperandAccessFacts(
          expression,
          null,
          vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.AccessBaseKind.OTHER,
          vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.AccessWindowKind.OTHER,
          false,
          null,
          false,
          null
      );
    }

    return new OperandAccessFacts(
        expression,
        read,
        accessBaseKind(read.accessKind()),
        accessWindowKind(read.windowKind()),
        matchesElementShape(read, idx, elementBits),
        storageFacts(read.regTensor()),
        read.readBitWidth() == elementBits,
        bindingFacts(read, idx)
    );
  }

  private boolean matchesElementShape(IssReadRegNode read,
                                      vadl.viam.graph.dependency.ForIdxNode idx,
                                      int elementBits) {
    // Read-side shape matching mirrors the write side so later evaluators can reason about exact
    // lane correspondence without duplicating offset decoding.
    if (read.windowKind() == IssReadRegNode.WindowKind.CHUNK) {
      return isLoopElementOffset(read.bitOffset(), idx, elementBits)
          && isConstantInt(read.bitWidth(), elementBits);
    }
    if (read.windowKind() == IssReadRegNode.WindowKind.FULL) {
      return isFullyIndexedElementAccess(read.accessorIndices(), idx);
    }
    return false;
  }
}
