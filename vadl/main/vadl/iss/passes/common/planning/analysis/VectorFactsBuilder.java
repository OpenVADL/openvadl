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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.EffectFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.LoopFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.OperandAccessFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.OperationFacts;
import vadl.iss.passes.common.planning.analysis.VectorInstructionFacts.WriteAccessFacts;
import vadl.viam.Instruction;

/**
 * Mutable assembly object while extracting vector facts for one instruction.
 */
public final class VectorFactsBuilder {

  private final Instruction instruction;
  private int forallCount;
  private boolean hasSingleForallRegisterWriteBody;
  private int sideEffectCount;
  private @Nullable VectorCandidate candidate;
  private @Nullable WriteAccessFacts writeFacts;
  private @Nullable OperationFacts operationFacts;
  private final List<OperandAccessFacts> operandFacts = new ArrayList<>();

  public VectorFactsBuilder(Instruction instruction) {
    this.instruction = instruction;
  }

  public Instruction instruction() {
    return instruction;
  }

  public void setForallCount(int forallCount) {
    this.forallCount = forallCount;
  }

  public void setHasSingleForallRegisterWriteBody(boolean hasSingleForallRegisterWriteBody) {
    this.hasSingleForallRegisterWriteBody = hasSingleForallRegisterWriteBody;
  }

  public void setSideEffectCount(int sideEffectCount) {
    this.sideEffectCount = sideEffectCount;
  }

  public void setCandidate(VectorCandidate candidate) {
    this.candidate = candidate;
  }

  public @Nullable VectorCandidate candidate() {
    return candidate;
  }

  public void setWriteFacts(WriteAccessFacts writeFacts) {
    this.writeFacts = writeFacts;
  }

  public @Nullable WriteAccessFacts writeFacts() {
    return writeFacts;
  }

  public void setOperationFacts(OperationFacts operationFacts) {
    this.operationFacts = operationFacts;
  }

  public @Nullable OperationFacts operationFacts() {
    return operationFacts;
  }

  public void addOperandFact(OperandAccessFacts operandFact) {
    operandFacts.add(operandFact);
  }

  /**
   * Materializes the immutable vector fact set extracted for this instruction.
   */
  public VectorInstructionFacts toFacts() {
    return new VectorInstructionFacts(
        instruction,
        new LoopFacts(forallCount, hasSingleForallRegisterWriteBody),
        new EffectFacts(sideEffectCount),
        candidate,
        writeFacts,
        operationFacts,
        List.copyOf(operandFacts)
    );
  }
}
